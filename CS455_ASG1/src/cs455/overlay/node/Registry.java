/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import cs455.overlay.routing.RoutingEntry;
import cs455.overlay.routing.RoutingTable;
import cs455.overlay.transport.TCPConnectionThread;
import cs455.overlay.transport.TCPServer;
import cs455.overlay.util.InteractiveCommandParser;
import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.EventFactory;
import cs455.overlay.wireformats.NodeReportsOverlaySetupStatus;
import cs455.overlay.wireformats.OverlayNodeReportsTaskFinished;
import cs455.overlay.wireformats.OverlayNodeSendsDeregistration;
import cs455.overlay.wireformats.OverlayNodeSendsRegistration;
import cs455.overlay.wireformats.Protocol;


public class Registry extends TCPServer{

	// Instance variables **************
	/*
	 * Map to hold registered nodes. Object[0] is the OverlayRegisrty, Object[1] is the TCPConnection
	 */
	private Map<Integer, Object[]> registeredNodes = new HashMap<Integer, Object[]>();
	private EventFactory ef = EventFactory.getInstance();
	private List<Event> nodesCompleted = new ArrayList<Event>();
	private RoutingTable routingTable;
	// Set routing table size, default to 3 if not specified
	private int NR = 3;
	// Range of valid ID's for client Nodes
	static final int MIN = 0;
	static final int MAX = 127;

	// Superclass constructor **************
	public Registry(int port) {
		super(port);
	}


	public static void main (String args[]){

		Registry registry = new Registry(Integer.parseInt(args[0]));

		try {

			registry.listen();
			System.out.println("Registry now listening for clients on port: " + registry.getPort());

		} catch (IOException e) {

			System.out.println("Error listening for clients: ");
			e.printStackTrace();

		}

		// Start listening for commands
		if(registry.isListening()){
			new InteractiveCommandParser(registry);
		}


	}// END main **************

	/********************************************
	 * Superclass method
	 * Used to handle events from Clients
	 * @return void
	 ********************************************/
	public void onEvent(Event e, TCPConnectionThread client){

		switch (e.getType()){

		case Protocol.OVERLAY_NODE_SENDS_REGISTRATION:
			registerNode(e, client);
			break;

		case Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION:
			deregisterNode(e, client);
			break;

		case Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS:
			getSetupStatus(e);
			break;

		case Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED:
			nodeReportsFinish(e);
			break;

		default:
			System.out.println("Unrecognized event type received");
		}


	}// END onEvent **************


	/******************************************
	 *************** Event Types **************
	 ******************************************/

	/*
	 * RECEIVE FROM CLIENT
	 */

	private void nodeReportsFinish(Event e){

		OverlayNodeReportsTaskFinished taskFinish = (OverlayNodeReportsTaskFinished) e;
		nodesCompleted.add(taskFinish);
		if(nodesCompleted.size() == registeredNodes.size()){
			// All nodes have reported task finish
			requestSummary();
		}

	}

	private void getSetupStatus(Event e){

		NodeReportsOverlaySetupStatus nodeSetupStatus = (NodeReportsOverlaySetupStatus) e;
		int status = nodeSetupStatus.getStatus();

		if(status != -1){
			// Setup successful, status = nodeID
		}else{
			// Setup failed
			// Routing table is now void, need to rebuild
			routingTable.clear();
		}
	}

	private void deregisterNode(Event e, TCPConnectionThread client){
		int status = -1;
		OverlayNodeSendsDeregistration deregister = (OverlayNodeSendsDeregistration) e;
		if(registeredNodes.remove(deregister.getNodeID()) != null){
			status = 1;
		}
		Event deregisterStatus = ef.buildEvent(Protocol.REGISTRY_REPORTS_DEREGISTRATION_STATUS, "" + status);
		//System.out.println("Node " + deregister.getNodeID() + " deregistered with status " + status);
		try {
			// Send deregistration status back to client
			client.sendToClient(deregisterStatus.getBytes());
			// Routing table is now void, need to rebuild
			routingTable.clear();
		} catch (IOException e1) {
			System.out.println("Error sending deregistration status to client: ");
			e1.printStackTrace();
		}
	}

	private void registerNode(Event e, TCPConnectionThread client){
		/*
		 * Register Client node
		 * Attempts to register client node
		 * Gets random (unique) identifer
		 * Add to registeredNodes
		 * If already registered, return error
		 * message to client
		 * 
		 */

		int status = -1;
		String message = "";

		// Get the InetAddress of the sender (using split to get just the IP, ignoring the hostname)
		String compare = client.getInetAddress().toString().split("/")[1];

		// Get object so we can see the data
		OverlayNodeSendsRegistration clientNode = (OverlayNodeSendsRegistration) e;

		// Get the random ID for this Node
		Integer nodeID = getNodeID();

		// Build the MessagingNode Object
		Object[] messageNode = new Object[2];
		messageNode[0] = clientNode;
		messageNode[1] = client;

		// Make sure IP matches first
		if(clientNode.getipAddress().equals(compare)){
			// Then make sure Node not already in list of registered clients
			if(!registeredNodes.containsValue(messageNode)){
				registeredNodes.put(nodeID, messageNode);
				status = nodeID;
				// Set the message
				message = "Registration request successful. The number of messaging nodes currently constituting the overlay is (" + registeredNodes.size() + ")";
			}else{
				message = "Unable to register client, already in list of registered nodes.";
			}
		} else {
			// There is an IP mismatch
			message = "Unable to register client, the IP sent and the IP of the socket do not match.";
		}

		// Registration status event
		Event registrationStatus = ef.buildEvent(Protocol.REGISTRY_REPORTS_REGISTRATION_STATUS, status + ";" + message.length() + ";" + message);

		try {
			client.setThreadID(nodeID);
			client.sendToClient(registrationStatus.getBytes());
		} catch (IOException exc) {
			System.out.println("Error sending data to client: ");
			exc.printStackTrace();
			/*
			 * In the rare case that a messaging node fails just after it sends a registration request, the
			 * registry will not be able to communicate with it. In this case, the entry for the messaging node should
			 * be removed from the data structure maintained at the registry.
			 */
			registeredNodes.remove(nodeID);
		}

	}

	/*
	 * SEND TO CLIENT
	 */

	// Called by getSetupStatus method once all nodes report task finish
	private void requestSummary(){
		Event requestSummary = ef.buildEvent(Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY, "");
		for (Integer key : registeredNodes.keySet()) {
			try {
				( (TCPConnectionThread) registeredNodes.get(key)[1] ).sendToClient(requestSummary.getBytes());
			} catch (IOException e) {
				System.out.println("Error sending requesting traffic summary to clients: ");
				e.printStackTrace();
			}
		}
	}

	// Called by command parser, needs to be public
	public void requestTaskInitiate(int numMessages){

		if(!routingTable.isEmpty()){

			System.out.println("Starting task with " + numMessages + " packets...");

			Event intiateTask = ef.buildEvent(Protocol.REGISTRY_REQUESTS_TASK_INITIATE, "" + numMessages);

			for (Integer key : registeredNodes.keySet()) {
				try {
					( (TCPConnectionThread) registeredNodes.get(key)[1] ).sendToClient(intiateTask.getBytes());
				} catch (IOException e) {
					System.out.println("Error sending task initate message to clients: ");
					e.printStackTrace();
				}
			}

		}else{
			System.out.println("The overlay has not yet been setup, please use \"setup-overlay [number-of-messages]\" command first to setup the overlay");
		}

	}

	// Called by command parser, needs to be public
	public void sendNodeManifest(){

		if(registeredNodes.size() > 1){
			// A list to hold each entry as a RoutingEntry
			List<RoutingEntry> entries = new ArrayList<RoutingEntry>();

			// Get each registered node and build a routing entry
			for (Entry<Integer, Object[]> entry : registeredNodes.entrySet()) {

				OverlayNodeSendsRegistration val = (OverlayNodeSendsRegistration) entry.getValue()[0];
				int nodeID = entry.getKey();
				entries.add(new RoutingEntry(nodeID, val.getipAddressLength(), val.getipAddress(), val.getPortNum()));

			}

			// Build the routing table using the RoutingEntry list 
			// created above
			routingTable = new RoutingTable(NR, entries);


			//System.out.println("The nodes list is: " + rt.getNodesList());

			for (Integer key : registeredNodes.keySet()) {
				String message =  NR + ";" + routingTable.getNodesList() + ";" + routingTable.getNodesTable(key) + ";" + registeredNodes.size();
				Event e = ef.buildEvent(Protocol.REGISTRY_SENDS_NODE_MANIFEST, message);
				TCPConnectionThread client = (TCPConnectionThread)registeredNodes.get(key)[1];
				try {
					// System.out.println("Sending manifest to node: " + client.toString());
					client.sendToClient(e.getBytes());
				} catch (IOException e1) {
					System.out.println("Error sending manifest to client " + key + ": ");
					e1.printStackTrace();
				}
			}
		}else{
			System.out.println("Unable to setup overlay, you have " + registeredNodes.size() + " node(s) registerted, need a minimum of two.");
		}

	}

	/******************************************
	 ************* END Event Types ************
	 ******************************************/


	/**
	 * Get a random nodeID
	 * @return Integer
	 */
	private Integer getNodeID(){

		// Get a random nodeID
		Random rand = new Random();
		Integer n = rand.nextInt(MAX - MIN) + MIN;
		// Make sure we don't have a collision
		while(registeredNodes.containsKey(n)){
			n = rand.nextInt(MAX - MIN) + MIN;
		}
		return n;

	}// END getNodeID **************

	/**
	 * Used for debugging
	 * Prints list of connected Nodes
	 * @return void
	 */
	public void printNodes(){

		/*
		 * Print out registered nodes
		 */

		if(registeredNodes.isEmpty()){
			System.out.println("There are currently no registered nodes.");
		}else{
			System.out.println("Currently registered nodes:");

			for (Entry<Integer, Object[]> entry : registeredNodes.entrySet()) {
				System.out.println("NodeID: " + entry.getKey() + ", " + entry.getValue()[0]);
			}
		}

		System.out.println();

	}// END printNodes **************

	/**
	 * Setter/Getter for NR size
	 * Called by the command parser
	 * @param size
	 */
	public void setNRSize(int size){
		NR = size;
	}
	public int getNRSize(){
		return NR;
	}

	public void listRoutingTables(){
		if(routingTable != null){
			System.out.println(routingTable.getRoutingTables());
		}else{
			System.out.println("Routing table not yet setup. Please issue the \"setup-overlay [num-routing-table-entries]\" command first.");
		}
	}

	protected void clientDisconnected(TCPConnectionThread client) {
		// Client disconnected, remove them from list of registered nodes
		registeredNodes.remove(client.getThreadID());
		// Routing table is now void, need to rebuild
		routingTable.clear();
	}

	protected void clientException(TCPConnectionThread client, Throwable exception) {
		// Client had connection exception, remove them from list of registered nodes
		registeredNodes.remove(client.getThreadID());
		// Routing table is now void, need to rebuild
		routingTable.clear();
	}

	/**
	 * Method for testing connection and message passing
	 */
	public void sendBunk(){
		String bunk = "SERVER> bunk!";

		for (Integer key : registeredNodes.keySet()) {
			try {
				( (TCPConnectionThread) registeredNodes.get(key)[1] ).sendToClient(bunk.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


	}

} // ************** END Registry class **************
