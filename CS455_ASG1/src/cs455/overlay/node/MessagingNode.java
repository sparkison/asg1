/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;

import cs455.overlay.routing.RoutingEntry;
import cs455.overlay.transport.TCPReceiverThread;
import cs455.overlay.transport.TCPSender;
import cs455.overlay.util.InteractiveCommandParser;
import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.EventFactory;
import cs455.overlay.wireformats.OverlayNodeSendsData;
import cs455.overlay.wireformats.Protocol;
import cs455.overlay.wireformats.RegistryReportsDeregistrationStatus;
import cs455.overlay.wireformats.RegistryReportsRegistrationStatus;
import cs455.overlay.wireformats.RegistryRequestsTaskInitiate;
import cs455.overlay.wireformats.RegistrySendsNodeManifest;

public class MessagingNode implements Node{

	// Instance variables **************
	private NavigableMap<Integer, TCPSender> clientConnections = new TreeMap<Integer, TCPSender>();
	private Queue<OverlayNodeSendsData> relayQueue = new LinkedList<OverlayNodeSendsData>();
	private EventFactory ef = EventFactory.getInstance();
	private String myIPAddress;
	private int myID;
	private int listenPort;
	private boolean debug = false;

	// Connections
	private Socket clientSocket;
	private ServerSocket svSocket;
	private TCPSender registrySender;
	private TCPReceiverThread receiver;
	private List<RoutingEntry> routingTable;
	private int[] nodeList;

	// Trackers
	private int sendTracker;
	private long sendSummation;
	private int receiveTraker;
	private long receiveSummation;
	private int relayTracker;

	// MessagingNode constructor
	public MessagingNode(String host, int port){

		try {

			// Setup connections (input/output streams)
			this.clientSocket = new Socket(host, port);
			this.registrySender = new TCPSender(clientSocket);
			/*
			 * The TCPReceiverThread takes an id first, since the 
			 * message node doesn't care who the data came from,
			 * don't need to use this.
			 */
			this.receiver = new TCPReceiverThread(0, clientSocket, this);
			Thread receive = new Thread(receiver);
			receive.start();

		} catch (UnknownHostException e) {
			e.printStackTrace();
			close();
		} catch (IOException e) {
			e.printStackTrace();
			close();
		}
		try {
			// Open ServerSocket to accept data from other Messaging Nodes
			this.svSocket = new ServerSocket(0);
			// Set listening port to pass to Registry
			this.listenPort = svSocket.getLocalPort();
			// Display success message
			System.out.println("Client listening for connections on port: " + listenPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Set IP address 
		myIPAddress = clientSocket.getLocalAddress().toString().split("/")[1];
		// Start the message handler thread to remove circular wait condition
		messageRouterThread();

	}

	public static void main(String args[]){
		// Construct the Messaging Node
		MessagingNode messageNode = new MessagingNode(args[0], Integer.parseInt(args[1]));
		// Spawn a thread to list for connections on the ServerSocket
		messageNode.listenThread();
		// Initialize this Messaging Node
		messageNode.intializeMessageNode();
	}

	private void intializeMessageNode(){
		// Build a new registration event
		Event registerClient = ef.buildEvent(Protocol.OVERLAY_NODE_SENDS_REGISTRATION, 
				myIPAddress.length()+";"+myIPAddress+";" + listenPort);

		// Finally, send the message to the server
		try {
			registrySender.sendData(registerClient.getBytes());
		} catch (IOException e) {
			System.out.println("Error sending Registration Event to Registry: ");
			e.printStackTrace();
			System.exit(-1);
		}

		// Initiate the command parser to accept commands from the Terminal
		new InteractiveCommandParser(this);
	}

	/**
	 * Message handler
	 * Deals with messages that need routed to other clients
	 * Using Queue to prevent deadlock due to circular wait
	 */
	public void messageRouterThread(){
		Thread router = new Thread(new Runnable() {
			public void run() {
				while(true){
					OverlayNodeSendsData relayMsg;
					synchronized(relayQueue){
						relayMsg = relayQueue.poll();
					}
					if(relayMsg != null){
						try {
							updateRelayed();
							// Update the dissemination for this packet
							relayMsg.updateHopLength();
							relayMsg.updateHopTrace(myID);
							int sink = relayMsg.getDestinationID();
							int nearestNeighbor = getNearestNeighbor(sink);
							synchronized(clientConnections){
								clientConnections.get(nearestNeighbor).sendData(relayMsg.getBytes());
							}
						} catch (IOException e) {
							System.out.println("Error sending relay message to client: ");
							System.err.println(e.getMessage());
						}
					}
				}
			}
		});  
		router.start();
	}

	/**
	 * Listen method with embedded Thread class to
	 * start listening for client connections
	 */
	public void listenThread(){
		// "this" reference to use for spawning the listening Thread
		final MessagingNode messageNode = this;
		// "listener" Thread to accept incoming connections
		Thread listener = new Thread(new Runnable() {
			public void run() {
				while(true){
					try {

						Socket client = svSocket.accept();
						/*
						 * The messaging node doesn't need to assign id's
						 * for record tracking, so defaulting to 0 for connectionID
						 */
						synchronized(this){
							Thread receive = new Thread(new TCPReceiverThread(0, client, messageNode));
							receive.start();
						}							

					} catch (IOException e) {}
				}
			}
		});  
		listener.start();
	}

	@Override
	public void onEvent(Event event, int id) {

		switch (event.getType()){

		case Protocol.REGISTRY_REPORTS_REGISTRATION_STATUS:
			registerStatusEvent(event);
			break;

		case Protocol.REGISTRY_REPORTS_DEREGISTRATION_STATUS:
			deregisterStatusEvent(event);
			break;

		case Protocol.REGISTRY_SENDS_NODE_MANIFEST:
			setupRoutingTable(event);
			break;

		case Protocol.REGISTRY_REQUESTS_TASK_INITIATE:
			startTask(event);
			break;

		case Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY:
			getStats();
			break;

		case Protocol.OVERLAY_NODE_SENDS_DATA:
			dataFromMessageNode(event);
			break;

		default:
			System.out.println("Unrecognized event type received");

		}

	}// END onEvent **************

	/******************************************
	 *************** Event Types **************
	 ******************************************/

	/*
	 * RECEIVE FROM REGISTRY
	 */

	/**
	 * Receives the registration status from the Registry
	 * @param event
	 */
	private void registerStatusEvent(Event event){
		RegistryReportsRegistrationStatus registerStatus = (RegistryReportsRegistrationStatus) event;
		if(registerStatus.getStatus() != -1){

			// Success!
			myID = registerStatus.getStatus();
			System.out.println("Registration successful, assigned id: " + myID);

		} else {
			// Unable to register
			System.out.println("Registration error!");
			System.out.println(registerStatus.getMessage());
			close();
		}

	}

	/**
	 * Gets de-registration status from Registry
	 * @param event
	 */
	private void deregisterStatusEvent(Event event){

		RegistryReportsDeregistrationStatus deregister = (RegistryReportsDeregistrationStatus) event;

		if(deregister.getDeregistrationStatus() == 1){
			System.out.println("Node " + myID + " successfully deregistered.");
		} else {
			System.out.println("Unable to deregister with Registry");
		}
		close();
	}

	/**
	 * Setup the routing table for this node
	 * @param event
	 */
	private void setupRoutingTable(Event event){

		RegistrySendsNodeManifest nodeManifest = (RegistrySendsNodeManifest) event;
		routingTable = nodeManifest.getRoutingEntries();
		nodeList = nodeManifest.getAllNodes();

		/*
		 * Status message variables
		 * Optimistic by default, change if
		 * Unsuccessful on connection setup to any node
		 * in this nodes routing table
		 */
		String statusMessage = "Setup successful";
		int status = myID;

		// Connect to clients, based on this nodes routing table
		for(RoutingEntry node : routingTable){
			Socket socket;
			try {
				socket = new Socket(node.getIpAddress(), node.getPortNum());
				clientConnections.put(node.getNodeID(), new TCPSender(socket));
			} catch (UnknownHostException e) {
				statusMessage = "Setup failed";
				status = -1;
				System.out.println("Error connecting to client, unknown host error occurred: ");
				e.printStackTrace();
			} catch (IOException e) {
				statusMessage = "Setup failed";
				status = -1;
				System.out.println("Error connecting to client: ");
				e.printStackTrace();
			}

		}

		// Notify Registry of setup status
		Event setupStatus = ef.buildEvent(Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS, status + ";" + statusMessage.length() + ";" + statusMessage);
		try {
			registrySender.sendData(setupStatus.getBytes());
		} catch (IOException e) {
			System.out.println("Error sending setup status to Registry: ");
			e.printStackTrace();
		}

	}

	/**
	 * Method called when Registry request task initiate
	 * @param event
	 */
	private void startTask(Event event){

		RegistryRequestsTaskInitiate taskInitiate = (RegistryRequestsTaskInitiate) event;
		int numPackets = taskInitiate.getNumPackets();

		int payload;
		int sink;

		/*
		 * Setting hop to source initially, specs say not to, but makes 
		 * it easier to track packet from source to sink...
		 * hopLength, however, is the actual number of intermediate hops
		 * from source to sink
		 */
		String hop = "(SRC: " + myID + ")";
		int hopLength = 0;

		/*
		 * Set/Reset the tracker variables
		 * so the "start" command can be issued multiple times
		 */
		synchronized(this){
			sendTracker 		= 0;
			sendSummation 		= 0;
			receiveTraker 		= 0;
			receiveSummation 	= 0;
			relayTracker 		= 0;
		}

		for(int i = 0; i<numPackets; ++i){

			// Get payload and select node to send to
			payload = getPayload();
			sink = selectRandomNode();

			// Debugging
			if(debug)
				System.out.println("Sending data to node: " + sink);

			// Track the sent tracker
			updateSent(payload);

			Event data = ef.buildEvent(Protocol.OVERLAY_NODE_SENDS_DATA, sink + ";" + myID + ";" + payload + ";" + hopLength + ";" + hop);

			if(clientConnections.containsKey(sink)){
				sendDataToClient(sink, data);
			}else{
				sendDataToClient(getNearestNeighbor(sink), data);
			}
		}
		// If here, task finished, notify Registry
		Event finishStatus = ef.buildEvent(Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED, myIPAddress + ";" + listenPort + ";" + myID);
		try {
			registrySender.sendData(finishStatus.getBytes());
		} catch (IOException e) {
			System.out.println("Error sending task complete status to Registry: ");
			e.printStackTrace();
		}

		if(debug){
			System.out.println("**********************************");
			System.out.println("Node " + myID + " finished task!");
			System.out.println("**********************************");
		}

	}

	/**
	 * Called when data is sent to this node
	 * from another MessageNode
	 * @param event
	 */
	public void dataFromMessageNode(Event data){
		OverlayNodeSendsData relayMsg = (OverlayNodeSendsData) data;
		int sink = relayMsg.getDestinationID();

		if(sink == myID){
			updateReceived(relayMsg.getPayLoad());
			relayMsg.updateHopTrace(myID);
			// Debugging
			if(debug){
				System.out.println("Received payload for node (" + myID + ")!!");
				System.out.println("Trace: num hops = " + relayMsg.getHopTraceLength() + ", trace route = " + relayMsg.getHopTrace());
				System.out.println();
			}
		}else{
			synchronized(relayQueue){
				// Route the packet
				relayQueue.add(relayMsg);
			}
		}
	}

	/**
	 * Method used to route data to
	 * nearest neighbor
	 * @param sink
	 * @param data
	 */
	private int getNearestNeighbor(int sink){
		/*
		 * Node not in list of connections, need to find nearest node
		 * Idea: take sink - nodeID, if negative, we passed the sink
		 * ignore it, else compare to min, if less set to min, continue
		 * After comparing all clientConnections end up with nearest node
		 */
		int min = Integer.MAX_VALUE;
		int compare;
		int location = -1;
		for (Integer key : clientConnections.keySet()) {
			compare = sink - key;
			if(compare < min && compare > 0){
				min = compare;
				location = key;
			}
		}
		/*
		 * If location is still -1, the destination node is behind us (looped around in the list)
		 * Need to determine based on max(Math.abs(a,b))
		 * Is there a better way to do this in a single loop iteration???
		 */
		if(location == -1){
			int max = Integer.MIN_VALUE;
			for (Integer key : clientConnections.keySet()) {
				compare = Math.abs(sink - key);
				if(compare > max){
					max = compare;
					location = key;
				}
			}
		}
		return location;
	}

	/*
	 * Simple helper method to send data to a client
	 */
	private void sendDataToClient(int id, Event data){
		synchronized(clientConnections){
			try {
				clientConnections.get(id).sendData(data.getBytes());
			} catch (IOException e) {
				System.out.println("Error sending payload to client: ");
				e.printStackTrace();
			}
		}
	}

	/*
	 * Select a random node to send data to
	 * not equal to self
	 */
	private int selectRandomNode(){
		Random rand = new Random();
		int r = rand.nextInt(nodeList.length);
		while(nodeList[r] == myID){
			r = rand.nextInt(nodeList.length);
		}
		return nodeList[r];
	}

	/*
	 * Get a random payload to send
	 */
	private int getPayload(){
		Random rand = new Random();		
		return rand.nextInt();
	}

	/*
	 * STATISTIC UPDATER METHODS
	 */
	private synchronized void updateSent(long payload){
		sendTracker++;
		sendSummation += payload;
	}
	private synchronized void updateRelayed(){
		relayTracker++;
	}
	private synchronized void updateReceived(int payload){
		receiveTraker++;
		receiveSummation += payload;
	}

	/*
	 * SEND TO REGISTRY
	 */

	/**
	 * Send deregistration event to Registry
	 */
	public void sendDeregistration(){
		String message = myIPAddress.length() + ";" + myIPAddress + ";" + listenPort + ";" + myID;
		Event e = ef.buildEvent(Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION, message);
		try {
			registrySender.sendData(e.getBytes());
		} catch (IOException e1) {
			System.out.println("Error sending deregistration to Registry: ");
			e1.printStackTrace();
		}

	}

	/**
	 * Send statistics summary event to Registry
	 */
	private void getStats(){

		Event reportSummary = ef.buildEvent(Protocol.OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY, myID 
				+ ";" + sendTracker + ";" + relayTracker + ";" + sendSummation 
				+ ";" + receiveTraker + ";" + receiveSummation);
		try {
			registrySender.sendData(reportSummary.getBytes());
		} catch (IOException e) {
			System.out.println("Error sending report summary to Registry: ");
			e.printStackTrace();
		}

	}

	/******************************************
	 ************* END Event Types ************
	 ******************************************/

	/**
	 * List the routing table for this node
	 */
	public void listRoutingTable(){
		if(routingTable != null){
			System.out.print("\nNodes constituting the overlay: ");
			for(int i = 0; i<nodeList.length; ++i){
				if(i != nodeList.length-1)
					System.out.print(nodeList[i] + ", ");
				else
					System.out.print(nodeList[i]);
			}
			System.out.println();
			System.out.println("Routing table for node (" + myID + "):");
			for(RoutingEntry entry : routingTable){
				System.out.println(entry);
			}
			System.out.println();
		}else{
			System.out.println("Routing table not yet sent");
		}

	}

	/**
	 * Prints out this nodes statistics
	 */
	public void printCounters(){

		System.out.println();
		System.out.println("Counters and diagnostics for node (" + myID + ")");
		System.out.println("Total packets sent: " + sendTracker);
		System.out.println("Total packets relayed: " + relayTracker);
		System.out.println("Sum of packet data sent: " + sendSummation);
		System.out.println("Total packets received: " + receiveTraker);
		System.out.println("Sum of packets received: " + receiveSummation);
		System.out.println();

	}

	/**
	 * Enabled/Disable debugging output
	 */
	public void setDebug(){
		if(debug){
			debug = false;
			System.out.println("Debugging disabled for this node");
		}else{
			debug = true;
			System.out.println("Debugging enabled for this node");
		}

	}

	/**
	 * Close socket connections and exit
	 */
	public void close(){
		try {
			clientSocket.close();
			svSocket.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		System.exit(0);

	}

}// ************** END MessagingNode class **************