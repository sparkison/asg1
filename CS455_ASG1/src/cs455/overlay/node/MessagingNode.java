/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.node;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cs455.overlay.routing.RoutingEntry;
import cs455.overlay.transport.TCPClient;
import cs455.overlay.transport.TCPConnectionThread;
import cs455.overlay.transport.TCPServer;
import cs455.overlay.util.InteractiveCommandParser;
import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.EventFactory;
import cs455.overlay.wireformats.OverlayNodeSendsData;
import cs455.overlay.wireformats.Protocol;
import cs455.overlay.wireformats.RegistryReportsDeregistrationStatus;
import cs455.overlay.wireformats.RegistryReportsRegistrationStatus;
import cs455.overlay.wireformats.RegistryRequestsTaskInitiate;
import cs455.overlay.wireformats.RegistrySendsNodeManifest;


public class MessagingNode extends TCPClient {

	// Instance variables **************
	private	EventFactory ef = EventFactory.getInstance();
	// This is the TCPServerThread for this client
	private ClientReceiver clientReceiver;
	private String myIPAddress;
	private int myID;
	private int listenPort;
	private List<RoutingEntry> routingTable;
	private int[] nodeList;
	private int sendTracker;
	private long sendSummation;

	// Superclass constructor **************
	public MessagingNode(String host, int port) {
		super(host, port);
	}


	public static void main (String args[]){

		// Establish connection to the server...
		MessagingNode messengerClient = new MessagingNode(args[0], Integer.parseInt(args[1]));

		// Start client
		messengerClient.startClient();

		// Delay for a second to allow server to respond
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// Accept user input
		new InteractiveCommandParser(messengerClient);

	}// END Main **************

	public void startClient(){
		/*
		 * Setup connection with server
		 * If successful, will start listening for connections
		 */

		try {
			this.openConnection();
			/*
			 * Try to setup new TCPServerThread
			 * to accept connections from other client Nodes
			 * 
			 * Need to set this up first so we know
			 * what our port will be
			 */
			initilizeReceiver(0);

			// Get InetAddress (using split to get just the IP, ignoring the hostname) 
			// had issues with it being blank on one end and not the other
			myIPAddress = getInetAddress().toString().split("/")[1];

			// Build a new registration event
			Event registerClient = ef.buildEvent(Protocol.OVERLAY_NODE_SENDS_REGISTRATION, 
					myIPAddress.length()+";"+myIPAddress+";" + listenPort);

			// Finally, send the message to the server
			sendToServer(registerClient.getBytes());

		} catch (IOException e1) {
			System.out.println("Error connecting and sending data to Registry: ");
			e1.printStackTrace();
			try {
				clientReceiver.close();
			} catch (IOException e) {
				System.out.println("Error closing Clients TCPServerThread down: ");
				e.printStackTrace();
			}
		}		

	}// END startClient **************

	/**
	 * Used to initialize the server thread
	 * to listen for client Node connections
	 * @param Port
	 * @return void
	 */
	private void initilizeReceiver(int port){
		clientReceiver = new ClientReceiver(port);
		try {
			clientReceiver.listen();
			listenPort = clientReceiver.getPort();
			System.out.println("Client listening for connections on port: " + listenPort);
		} catch (IOException e) {
			System.out.println("Client had error listening for connections: ");
			e.printStackTrace();
		}

	}// END initilizeReceiver **************

	/********************************************
	 * Overloaded method
	 * dealing with the server
	 * @param Event
	 ********************************************/
	public void onEvent(Event event){

		// System.out.println("New event received from Server:\n" + e + "\n");

		switch (event.getType()){

		case Protocol.REGISTRY_REPORTS_REGISTRATION_STATUS:
			registerStatusEvent(event);
			break;

		case Protocol.REGISTRY_REPORTS_DEREGISTRATION_STATUS:
			System.out.println();
			deregisterStatusEvent(event);
			break;

		case Protocol.REGISTRY_SENDS_NODE_MANIFEST:
			setupRoutingTable(event);
			break;

		case Protocol.REGISTRY_REQUESTS_TASK_INITIATE:
			try {
				startTask(event);
			} catch (UnknownHostException e1) {
				System.out.println("Unknown host error: ");
				e1.printStackTrace();
			} catch (IOException e1) {
				System.out.println("IO Exception error: ");
				e1.printStackTrace();
			}
			break;

		case Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY:
			// For testing, only need getStats
			printCounters();
			getStats();
			break;

		default:
			System.out.println("Unrecognized event type received");

		}		


	}// END onEvent **************


	/******************************************
	 *************** Event Types **************
	 ******************************************/

	/*
	 * RECEIVE FROM SERVER
	 */

	// Get registration status
	// Called within class
	private synchronized void registerStatusEvent(Event e){
		RegistryReportsRegistrationStatus registerStatus = (RegistryReportsRegistrationStatus) e;
		if(registerStatus.getStatus() != -1){

			// Success!
			myID = registerStatus.getStatus();
			clientReceiver.setMyID(myID);
			System.out.println("Registration successful, assigned id: " + myID);

		} else {
			// Unable to register
			System.out.println("Registration error");
			close();
		}

	}

	// Get deRegistration status
	// Called within class
	private void deregisterStatusEvent(Event e){

		RegistryReportsDeregistrationStatus deregister = (RegistryReportsDeregistrationStatus) e;

		if(deregister.getDeregistrationStatus() == 1){
			System.out.println("Node " + myID + " successfully deregistered.");
		} else {
			System.out.println("Unable to deregister with Registry");
		}
		close();
	}

	// Get routing table
	// Called within class
	private void setupRoutingTable(Event e){

		RegistrySendsNodeManifest nodeManifest = (RegistrySendsNodeManifest) e;

		routingTable = nodeManifest.getRoutingEntries();
		nodeList = nodeManifest.getAllNodes();


	}

	// Attempt to initiate task
	// Called within class
	private synchronized void startTask(Event e) throws UnknownHostException, IOException{

		Map<Integer, TCPConnectionThread> clientConnections = new HashMap<Integer, TCPConnectionThread>();

		RegistryRequestsTaskInitiate taskInitiate = (RegistryRequestsTaskInitiate) e;

		for(RoutingEntry node : routingTable){
			Socket socket = new Socket(node.getIpAddress(), node.getPortNum());
			clientConnections.put(node.getNodeID(), new TCPConnectionThread(clientReceiver.getThreadGroup(), socket, clientReceiver));
		}
		
		int numPackets = taskInitiate.getNumPackets();
		
		// Update the receiver with list of connections
		clientReceiver.setRoutingTable(clientConnections);				

		// If here, setup was successful, send success message to Registry
		//System.out.println("Node (" + myID + ") starting task with " + numPackets + " packets");
		String statusMessage = "Setup successful";
		Event setupStatus = ef.buildEvent(Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS, myID + ";" + statusMessage.length() + ";" + statusMessage);
		this.sendToServer(setupStatus.getBytes());

		int payload;
		int sink;
		Random rand = new Random();

		// Since this is the initial packet don't need to worry about
		// hop-trace, set it empty to begin with
		String hop = " ";
		int hopLength = 0;

		// Set/Reset the tracker variables
		sendTracker = 0;
		sendSummation = 0;

		//System.out.println("Getting ready to loop for " + numPackets + " packets");

		for(int i = 0; i<numPackets; ++i){
			
			// Get payload and select node to send to
			payload = getPayload();
			sink = selectRandomNode(rand);
			// Track the data for traffic summary
			updateCounts(payload);

			Event data = ef.buildEvent(Protocol.OVERLAY_NODE_SENDS_DATA, sink + ";" + myID + ";" + payload + ";" + hopLength + ";" + hop);

			if(clientConnections.containsKey(sink)){
				clientConnections.get(sink).sendToClient(data.getBytes());
			}else{				
				// Node not in list of connections, need to find nearest node
				// Idea: take sink - nodeID, if negative, we passed the sink
				// ignore it, else compare to min, if less set to min, continue
				// After comparing all clientConnections end up with nearest node

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

				// If location is still -1, the destination node is behind us (looped around in the list)
				// Need to determine based on Math.abs(a,b)
				// Is there a better way to do this in a single loop iteration???

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
					clientConnections.get(location).sendToClient(data.getBytes());
				else
					System.out.println("Error, node not in list of neighbors.");
			}
		}

		// If here, task finished
		Event finishStatus = ef.buildEvent(Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED, myIPAddress + ";" + listenPort + ";" + myID);
		this.sendToServer(finishStatus.getBytes());

	}

	// Helpers for above, called by startTask method
	private int selectRandomNode(Random rand){
		int r = rand.nextInt(nodeList.length);
		while(nodeList[r] == myID){
			r = rand.nextInt(nodeList.length);
		}
		return nodeList[r];
	}
	// Get a random payload between 
	// Integer.MAX_VALUE and Integer.MIN_VALUE
	private int getPayload(){
		Random rand = new Random();		
		return rand.nextInt();
	}
	private synchronized void updateCounts(long payload){
		sendSummation += payload;
		sendTracker++;
	}

	/*
	 * SEND TO SERVER
	 */

	// Setup deRegistration message
	// Called by CommandParser, need to be public
	public void sendDeregistration(){
		int listenPort = clientReceiver.getPort();
		String message = myIPAddress.length() + ";" + myIPAddress + ";" + listenPort + ";" + myID;
		Event e = ef.buildEvent(Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION, message);
		try {
			this.sendToServer(e.getBytes());
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
	 * Method for testing connection and message passing
	 */
	public void sendBunk(){
		String bunk = "CLIENT("+myID+")> bunk!";
		try {
			this.sendToServer(bunk.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send stats to Registry
	 */
	private void getStats(){





		// ************** TO-DO **************





	}

	/**
	 * Prints out this nodes statistics
	 */
	public void printCounters(){

		System.out.println();
		System.out.println("Counters and diagnostics for node (" + myID + ")");
		System.out.println("Total packets sent: " + sendTracker);
		System.out.println("Total packets relayed: " + clientReceiver.getRelayTracker());
		System.out.println("Sum of packet data sent: " + sendSummation);
		System.out.println("Total packets received: " + clientReceiver.getReceiveTraker());
		System.out.println("Sum of packets received: " + clientReceiver.getReceiveSummation());
		System.out.println();

	}

	protected void connectionClosed() {
		System.out.println("The connection was closed");
		System.exit(-1);
	}

	protected void connectionException(Exception exception) {
		System.out.println("Connection with server lost, closing client");
		exception.printStackTrace();
		System.exit(-1);
	}

	/**
	 * Close connections
	 */
	public void close(){
		try {
			// Shut it down!
			this.closeConnection();
			clientReceiver.close();
		} catch (IOException e1) {
			System.out.println("Error closing connection with server: ");
			e1.printStackTrace();
		}
		System.exit(1);
	}


	/********************************************
	 * Nested sub-class
	 * Used to receive connections
	 * from other client Nodes
	 * 
	 * Also used to track packet info
	 * 
	 * @author Shaun Parkison (shaunpa)
	 *
	 ********************************************/

	private class ClientReceiver extends TCPServer {

		// Instance variables **************
		private int myID;
		private int receiveTraker;
		private long receiveSummation;
		private int relayTracker;
		private Map<Integer, TCPConnectionThread> clientConnections;

		// Variables for holding message receiving stats...

		public ClientReceiver(int port) {
			super(port);
		}

		protected void clientDisconnected(TCPConnectionThread client) {
			// Client disconnected
			//System.out.println("Client disconnected");
		}

		protected void clientException(TCPConnectionThread client, Throwable exception) {
			// Client had connection exception
			//System.out.println("Client lost connection");
		}

		@Override
		public void onEvent(Event event, TCPConnectionThread client) {

			OverlayNodeSendsData ovnData;

			try {
				ovnData = new OverlayNodeSendsData(event.getBytes());

				if(ovnData.getDestinationID() == myID){
					
					updateReceived(ovnData.getPayLoad());
					
					// Debugging
					System.out.println("Received payload!");
					System.out.println("Trace: packet trace length = " + ovnData.getHopTraceLength() + ", hop trace = " + ovnData.getHopTrace());
					System.out.println();
				}else{
					forwardPacket(ovnData);
				}

			} catch (IOException e) {
				System.out.println("Error getting data: ");
				e.printStackTrace();
			}

		}

		private void forwardPacket(OverlayNodeSendsData ovnData) throws IOException{
			
			updateRelay();
			int sink = ovnData.getDestinationID();
			
			// Update the dissemination for this packet
			ovnData.updateHopLength();
			ovnData.updateHopTrace(myID);
			
			if(clientConnections.containsKey(sink)){
				clientConnections.get(sink).sendToClient(ovnData.getBytes());
			}else{				
				// Node not in list of connections, need to find nearest node
				// Idea: take sink - nodeID, if negative, we passed the sink
				// ignore it, else compare to min, if less set to min, continue
				// After comparing all clientConnections end up with nearest node

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

				// If location is still -1, the destination node is behind us (looped around in the list)
				// Need to determine based on Math.abs(a,b)
				// Is there a better way to do this in a single loop iteration???

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
					clientConnections.get(location).sendToClient(ovnData.getBytes());
				else
					System.out.println("Error, node not in list of neighbors.");
			}
		}
		
		private synchronized void updateReceived(long payload){
			receiveSummation += payload;
			receiveTraker++;
		}
		
		private synchronized void updateRelay(){
			relayTracker++;
		}

		/**
		 * SETTERS
		 */

		public void setMyID(int id){
			myID = id;
		}

		public void setRoutingTable(Map<Integer, TCPConnectionThread> clientConnections){
			this.clientConnections = clientConnections;
			// if set new routing table, need to reset counters
			this.receiveTraker = 0;
			this.receiveSummation = 0;
			this.relayTracker = 0;
		}

		/**
		 * GETTERS
		 */

		public int getRelayTracker(){
			return relayTracker;
		}
		
		public int getReceiveTraker(){
			return receiveTraker;
		}

		public long getReceiveSummation(){
			return receiveSummation;
		}


	}// ************** END ClientReceiver sub-class **************

}// ************** END MessagingNode class **************