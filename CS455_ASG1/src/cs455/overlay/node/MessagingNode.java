/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.node;

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
	private Map<Integer, TCPConnectionThread> clientConnections = new HashMap<Integer, TCPConnectionThread>();
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

		// Accept user input
		new InteractiveCommandParser(this);

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
	 * Override method
	 * Used to handle events from Registry
	 * 
	 * @return void
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
			startTask(event);
			break;

		case Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY:
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
	private void registerStatusEvent(Event event){
		RegistryReportsRegistrationStatus registerStatus = (RegistryReportsRegistrationStatus) event;
		if(registerStatus.getStatus() != -1){

			// Success!
			myID = registerStatus.getStatus();
			clientReceiver.setMyID(myID);
			System.out.println("Registration successful, assigned id: " + myID);

		} else {
			// Unable to register
			System.out.println("Registration error!");
			System.out.println(registerStatus.getMessage());
			close();
		}

	}

	// Get deRegistration status
	// Called within class
	private void deregisterStatusEvent(Event event){

		RegistryReportsDeregistrationStatus deregister = (RegistryReportsDeregistrationStatus) event;

		if(deregister.getDeregistrationStatus() == 1){
			System.out.println("Node " + myID + " successfully deregistered.");
		} else {
			System.out.println("Unable to deregister with Registry");
		}
		close();
	}

	// Get routing table
	// Called within class
	private void setupRoutingTable(Event event){

		RegistrySendsNodeManifest nodeManifest = (RegistrySendsNodeManifest) event;
		routingTable = nodeManifest.getRoutingEntries();
		nodeList = nodeManifest.getAllNodes();
		// if new routing table sent, initialize/re-initialize connections
		clientConnections.clear();

	}

	// Attempt to initiate task
	// Called within class
	private void startTask(Event event){

		RegistryRequestsTaskInitiate taskInitiate = (RegistryRequestsTaskInitiate) event;
		int numPackets = taskInitiate.getNumPackets();

		// Status message variables
		String statusMessage = "Setup successful";
		int status = myID;

		if(clientConnections == null || clientConnections.isEmpty()){
			for(RoutingEntry node : routingTable){
				Socket socket;
				try {
					socket = new Socket(node.getIpAddress(), node.getPortNum());
					clientConnections.put(node.getNodeID(), new TCPConnectionThread(clientReceiver.getThreadGroup(), socket, clientReceiver));
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

		// Update the receiver with list of connections
		clientReceiver.setRoutingTable(clientConnections);					
		Event setupStatus = ef.buildEvent(Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS, status + ";" + statusMessage.length() + ";" + statusMessage);

		try {
			this.sendToServer(setupStatus.getBytes());
		} catch (IOException e) {
			System.out.println("Error sending setup status to Registry: ");
			e.printStackTrace();
		}

		// If status is -1 we had an error setting up connections, don't initiate task!
		if(status != -1){
			int payload;
			int sink;
			Random rand = new Random();

			/*
			 * Setting hop to source initially, specs say not to, but makes 
			 * it easier to track packet from source to sink...
			 * hopLength, however, is the actually number of intermediate hops
			 * from source to sink
			 */
			String hop = "(SRC: " + myID + ")";
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
					try {
						clientConnections.get(sink).sendToClient(data.getBytes());
					} catch (IOException e) {
						System.out.println("Error sending payload to client: ");
						e.printStackTrace();
					}
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
					// Need to determine based on max(Math.abs(a,b))
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
						try {
							clientConnections.get(location).sendToClient(data.getBytes());
						} catch (IOException e) {
							System.out.println("Error sending payload to client: ");
							e.printStackTrace();
						}
					else
						System.out.println("Error, node not in list of neighbors.");
				}
			}

			// If here, task finished, notify Registry
			Event finishStatus = ef.buildEvent(Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED, myIPAddress + ";" + listenPort + ";" + myID);
			try {
				this.sendToServer(finishStatus.getBytes());
			} catch (IOException e) {
				System.out.println("Error sending task complete status to Registry: ");
				e.printStackTrace();
			}
		}else{
			System.out.println("Error setting up connections with clients, unable to start task.");
		}

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
	// Update counters, ensuring concurrency
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
	 * Send statistics summary to Registry
	 */
	private void getStats(){

		Event reportSummary = ef.buildEvent(Protocol.OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY, myID 
				+ ";" + sendTracker + ";" + clientReceiver.getRelayTracker() + ";" + sendSummation 
				+ ";" + clientReceiver.getReceiveTraker() + ";" + clientReceiver.getReceiveSummation());
		try {
			this.sendToServer(reportSummary.getBytes());
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
		} finally {
			System.exit(1);
		}
		
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
			handleMessage(event);
		}

		final void handleMessage(Event event){
			OverlayNodeSendsData ovnData;

			try {
				ovnData = new OverlayNodeSendsData(event.getBytes());

				if(ovnData.getDestinationID() == myID){
					
					updateReceived(ovnData.getPayLoad());
					ovnData.updateHopTrace(myID);

					// Debugging
					//System.out.println("Received payload for node (" + myID + ")!!");
					//System.out.println("Trace: num hops = " + ovnData.getHopTraceLength() + ", trace route = " + ovnData.getHopTrace());
					//System.out.println();
				}else{
					forwardPacket(ovnData);
				}

			} catch (IOException e) {
				System.out.println("Error getting data: ");
				e.printStackTrace();
			}
		}

		final void forwardPacket(OverlayNodeSendsData ovnData) throws IOException{

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
				// Need to determine based on max(Math.abs(a,b))
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
