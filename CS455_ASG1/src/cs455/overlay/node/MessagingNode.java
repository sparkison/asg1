package cs455.overlay.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
	private Map<Integer, TCPSender> clientConnections = new HashMap<Integer, TCPSender>();
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

	}

	public static void main(String args[]){
		// Construct the Messaging Node
		MessagingNode messageNode = new MessagingNode(args[0], Integer.parseInt(args[1]));
		// Spawn a thread to list for connections on the ServerSocket
		messageNode.listen();
		// Initialize this Messaging Node
		messageNode.intializeMessageNode();
	}

	private void intializeMessageNode(){
		// Send registration event to Registry
		myIPAddress = clientSocket.getInetAddress().toString().split("/")[1];

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

	public void listen(){
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

					} catch (IOException e) {
						e.printStackTrace();
					}
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
		// if new routing table sent, initialize/re-initialize connections
		clientConnections.clear();

	}

	/**
	 * Method called when Registry request task initiate
	 * @param event
	 */
	private void startTask(Event event){

		RegistryRequestsTaskInitiate taskInitiate = (RegistryRequestsTaskInitiate) event;
		int numPackets = taskInitiate.getNumPackets();

		/*
		 * Status message variables
		 * Optimistic by default, change if
		 * Unsuccessful on connection setup to any node
		 * in this nodes routing table
		 */
		String statusMessage = "Setup successful";
		int status = myID;

		// Connect to clients, based on this nodes routing table
		if(clientConnections == null || clientConnections.isEmpty()){
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
		}

		// Notify Registry of setup status
		Event setupStatus = ef.buildEvent(Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS, status + ";" + statusMessage.length() + ";" + statusMessage);

		try {
			registrySender.sendData(setupStatus.getBytes());
		} catch (IOException e) {
			System.out.println("Error sending setup status to Registry: ");
			e.printStackTrace();
		}

		// If status is -1 we had an error setting up connections, don't initiate task!
		if(status != -1){
			sendDataToNodes(numPackets);
		}else{
			System.out.println("Error setting up connections with clients, unable to start task.");
		}

	}

	/**
	 * Helper method, initiates the loop and sends
	 * random payloads to random nodes
	 * @param numPackets
	 */
	private void sendDataToNodes(int numPackets){
		int payload;
		int sink;
		Random rand = new Random();
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
		sendTracker 		= 0;
		sendSummation 		= 0;
		receiveTraker 		= 0;
		receiveSummation 	= 0;
		relayTracker 		= 0;

		
		for(int i = 0; i<numPackets; ++i){

			// Get payload and select node to send to
			payload = getPayload();
			sink = selectRandomNode(rand);

			// Debugging
			if(debug)
				System.out.println("Sending data to node: " + sink);

			// Track the sent tracker
			updateSent(payload);

			Event data = ef.buildEvent(Protocol.OVERLAY_NODE_SENDS_DATA, sink + ";" + myID + ";" + payload + ";" + hopLength + ";" + hop);

			if(clientConnections.containsKey(sink)){
				try {
					clientConnections.get(sink).sendData(data.getBytes());
				} catch (IOException e) {
					System.out.println("Error sending payload to client: ");
					e.printStackTrace();
				}
			}else{
				routeData(sink, data);
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
		try {
			OverlayNodeSendsData ovnData = new OverlayNodeSendsData(data.getBytes());
			int sink = ovnData.getDestinationID();

			if(sink == myID){

				updateReceived(ovnData.getPayLoad());
				ovnData.updateHopTrace(myID);

				// Debugging
				if(debug){
					System.out.println("Received payload for node (" + myID + ")!!");
					System.out.println("Trace: num hops = " + ovnData.getHopTraceLength() + ", trace route = " + ovnData.getHopTrace());
					System.out.println();
				}

			}else{
				updateRelayed();
				// Update the dissemination for this packet
				ovnData.updateHopLength();
				ovnData.updateHopTrace(myID);
				// Route the packet
				
				/************************	DEADLOCK HERE!?!?!?!?	*****************************/
				/****	Getting deadlock on the send method, which uses TCPSender, why??	****/
				routeData(sink, data);  				
				
			}

		} catch (IOException e) {
			System.out.println("Error getting data: ");
			e.printStackTrace();
		}
	}

	/**
	 * Method used to route data to
	 * nearest neighbor
	 * @param sink
	 * @param data
	 */
	private void routeData(int sink, Event data){
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
		if(clientConnections.get(location) != null)
			sendDataToClient(location, data);
		else
			System.out.println("Error, node not in list of neighbors.");
	}

	private int selectRandomNode(Random rand){
		int r = rand.nextInt(nodeList.length);
		while(nodeList[r] == myID){
			r = rand.nextInt(nodeList.length);
		}
		return nodeList[r];
	}

	private int getPayload(){
		Random rand = new Random();		
		return rand.nextInt();
	}
	
	private void sendDataToClient(int id, Event data){
		try {
			clientConnections.get(id).sendData(data.getBytes());
		} catch (IOException e) {
			System.out.println("Error sending payload to client: ");
			e.printStackTrace();
		}
	}

	private synchronized void updateSent(long payload){
		sendSummation += payload;
		sendTracker++;
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
	 * Send statistics summary to Registry
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
			System.out.println("Debugging disabled for this node");
			debug = false;
		}else{
			System.out.println("Debugging enabled for this node");
			debug = true;
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
			e.printStackTrace();
		} finally {
			System.exit(-1);
		}

	}

}// ************** END MessagingNode class **************