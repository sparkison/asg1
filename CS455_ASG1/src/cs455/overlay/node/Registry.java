/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cs455.overlay.routing.RoutingEntry;
import cs455.overlay.routing.RoutingTable;
import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPConnectionCache;
import cs455.overlay.util.InteractiveCommandParser;
import cs455.overlay.util.StatisticsCollectorAndDisplay;
import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.EventFactory;
import cs455.overlay.wireformats.NodeReportsOverlaySetupStatus;
import cs455.overlay.wireformats.OverlayNodeReportsTaskFinished;
import cs455.overlay.wireformats.OverlayNodeSendsDeregistration;
import cs455.overlay.wireformats.OverlayNodeSendsRegistration;
import cs455.overlay.wireformats.Protocol;

public class Registry implements Node{

	// Instance variables **************
	private Map<Integer, OverlayNodeSendsRegistration> nodeRegistered = new HashMap<Integer, OverlayNodeSendsRegistration>();
	private StatisticsCollectorAndDisplay statistics = new StatisticsCollectorAndDisplay();
	private TCPConnectionCache connectionCache = new TCPConnectionCache();
	private List<Event> nodesCompleted = new ArrayList<Event>();
	private List<Event> nodesSummary = new ArrayList<Event>();
	private EventFactory ef = EventFactory.getInstance();
	private RoutingTable routingTable;
	private ServerSocket svSocket;
	private int port;	
	private int NR = 3;

	// Registry constructor
	public Registry(int port){
		
		this.port = port;
		
		try {
			// Open ServerSocket to accept data from Messaging Nodes
			svSocket = new ServerSocket(port);
			System.out.println("Registry now listening for clients on port: " + svSocket.getLocalPort());
		} catch (IOException e) {
			System.out.println("Error listening for clients: ");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void main(String args[]){
		// Construct the Registry Node
		Registry registry = new Registry(Integer.parseInt(args[0]));
		// Spawn a thread to list for connections on the ServerSocket
		registry.listen();
		// Initiate the command parser to accept commands from the Terminal
		new InteractiveCommandParser(registry);
	}

	public void listen(){
		// "this" reference to use for spawning the listening Thread
		final Registry registry = this;
		// "listener" Thread to accept incoming connections
		Thread listener = new Thread(new Runnable() {
			public void run() {

				while(true){
					try {
						/*
						 * Multiple nodes could be connection at the same time
						 */
						synchronized(this){
							Socket client = svSocket.accept();
							int key = connectionCache.getNodeID();
							TCPConnection clientConnection = new TCPConnection(key, client, registry);
							connectionCache.addConnection(key, clientConnection);
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

		case Protocol.OVERLAY_NODE_SENDS_REGISTRATION:
			registerNode(event, id);
			break;

		case Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION:
			deRegisterNode(event, id);
			break;

		case Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS:
			getSetupStatus(event, id);
			break;

		case Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED:
			nodeReportsFinish(event);
			break;

		case Protocol.OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY:
			nodeReportsSummary(event);
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

	/**
	 * Called when overlay node reports task complete
	 * Synchronized to ensure no concurrency issues as multiple
	 * messaging nodes could be sending at the same time
	 * @param event
	 */
	private synchronized void nodeReportsFinish(Event event){
		OverlayNodeReportsTaskFinished taskFinish = (OverlayNodeReportsTaskFinished) event;
		nodesCompleted.add(taskFinish);
		if(nodesCompleted.size() == nodeRegistered.size()){
			System.out.print("all nodes reported task complete waiting 5 seconds to request summary...");
			// All nodes have reported task finish
			try {
				/*
				 * Sleep for 5 seconds to allow threads time to finish up
				 * since it's possible clients have finished sending, but there
				 * may still be threads outstanding sending messages...
				 */
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			System.out.print("requesting data summary from each node.");
			requestSummary();
		}
	}

	/**
	 * Called when overlay node reports summary data
	 * Synchronized to ensure no concurrency issues as multiple
	 * messaging nodes could be sending at the same time
	 * @param event
	 */
	private synchronized void nodeReportsSummary(Event event){
		nodesSummary.add(event);
		if(nodesSummary.size() == nodeRegistered.size()){
			// All nodes have reported task finish
			statistics.sendNodeData(nodesSummary);
		}
	}

	/**
	 * Called when overlay node reports its setup status
	 * Will reset the routing table if unsuccessful to force
	 * Registry to re-issue "setup-overlay" command
	 * 
	 * Synchronized to ensure no concurrency issues as multiple
	 * messaging nodes could be sending at the same time
	 * @param event
	 */
	private synchronized void getSetupStatus(Event event, int id){

		NodeReportsOverlaySetupStatus nodeSetupStatus = (NodeReportsOverlaySetupStatus) event;
		int status = nodeSetupStatus.getStatus();

		if(status != -1){
			// Setup successful, status = nodeID
		}else{
			// Setup failed
			// Routing table is now void, need to rebuild
			System.out.println("Node " + id + " was unable to setup connections.");
			nodeRegistered.remove(id);
			connectionCache.removeConnection(id);
			resetRoutingTable();
		}
	}

	/**
	 * Called when an overlay node de-registers with the Registry
	 * @param event
	 * @param id
	 */
	private void deRegisterNode(Event event, int id){
		int status = -1;
		OverlayNodeSendsDeregistration deregister = (OverlayNodeSendsDeregistration) event;
		if(nodeRegistered.remove(deregister.getNodeID()) != null){
			status = 1;
		}
		Event deregisterStatus = ef.buildEvent(Protocol.REGISTRY_REPORTS_DEREGISTRATION_STATUS, "" + status);
		//System.out.println("Node " + deregister.getNodeID() + " deRegistered with status " + status);
		try {
			// Send deRegistration status back to client
			connectionCache.getConnection(id).sendData(deregisterStatus.getBytes());
			connectionCache.removeConnection(id);
			// Routing table is now void, need to rebuild
			resetRoutingTable();
		} catch (IOException e1) {
			System.out.println("Error sending deregistration status to client: ");
			e1.printStackTrace();
		}
	}
	
	/**
	 * Called when an overlay node request registration with the Registry
	 * @param event
	 * @param id
	 */
	private void registerNode(Event event, int id){

		/*
		 * Register Client node
		 * Attempts to register client node
		 * Gets random (unique) identifier
		 * Add to registeredNodes
		 * If already registered, return error
		 * message to client
		 * 
		 */

		int status = -1;
		String message = "";

		// Get the InetAddress of the sender (using split to get just the IP, ignoring the hostname)
		InetAddress inet = connectionCache.getInetAddress(id);
		String compare = inet.toString().split("/")[1];

		// Get object so we can see the data
		OverlayNodeSendsRegistration clientNode = (OverlayNodeSendsRegistration) event;

		// Make sure IP matches first
		if(clientNode.getipAddress().equals(compare)){
			// Then make sure Node not already in list of registered clients
			if(!nodeRegistered.containsKey(id)){
				nodeRegistered.put(id, clientNode);
				status = id;
				// Set the message
				message = "Registration request successful. The number of messaging nodes currently constituting the overlay is (" + nodeRegistered.size() + ")";
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
			connectionCache.getConnection(id).sendData(registrationStatus.getBytes());
		} catch (IOException e) {
			System.out.println("Error sending data to client: ");
			e.printStackTrace();
			/*
			 * In the rare case that a messaging node fails just after it sends a registration request, the
			 * registry will not be able to communicate with it. In this case, the entry for the messaging node should
			 * be removed from the data structure maintained at the registry.
			 * 
			 * Also removing from connections cache, as the connection is no longer valid.
			 */
			nodeRegistered.remove(id);
			connectionCache.removeConnection(id);
		}

	}

	/*
	 * SEND TO CLIENT
	 */

	/**
	 * Called by getSetupStatus method once all nodes report task finish
	 * Request all overlay nodes to send summary data
	 */
	private void requestSummary(){
		Event requestSummary = ef.buildEvent(Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY, "");
		connectionCache.sendToAll(requestSummary.getBytes(), "sending requesting traffic summary to clients");
	}

	/**
	 * Called by command parser, needs to be public
	 * Sends task initiate command to each overlay node
	 * @param numMessages
	 */
	public void requestTaskInitiate(int numMessages){
		if(routingTable == null || routingTable.isEmpty()){
			System.out.println("The overlay has not yet been setup, please use \"setup-overlay [number-of-messages]\" command first to setup the overlay");
		}else{
			// Reset completed and summary for new round
			nodesCompleted.clear();
			nodesSummary.clear();
			System.out.print("Starting task with " + numMessages + " packets...");
			Event intiateTask = ef.buildEvent(Protocol.REGISTRY_REQUESTS_TASK_INITIATE, "" + numMessages);
			connectionCache.sendToAll(intiateTask.getBytes(), "sending task initate message to clients");

		}

	}

	/**
	 * Called by command parser, needs to be public
	 * Sends node manifest to each overlay node
	 */
	public void sendNodeManifest(){

		if(nodeRegistered.size() > 1){
			// A list to hold each entry as a RoutingEntry
			List<RoutingEntry> entries = new ArrayList<RoutingEntry>();

			// Get each registered node and build a routing entry
			for (Entry<Integer, OverlayNodeSendsRegistration> entry : nodeRegistered.entrySet()) {

				OverlayNodeSendsRegistration val = entry.getValue();
				int nodeID = entry.getKey();
				entries.add(new RoutingEntry(nodeID, val.getipAddressLength(), val.getipAddress(), val.getPortNum()));

			}

			// Build the routing table using the RoutingEntry list 
			// created above
			routingTable = new RoutingTable(NR, entries);


			//System.out.println("The nodes list is: " + rt.getNodesList());

			for (Integer key : nodeRegistered.keySet()) {
				String message =  NR + ";" + routingTable.getNodesList() + ";" + routingTable.getNodesTable(key) + ";" + nodeRegistered.size();
				Event e = ef.buildEvent(Protocol.REGISTRY_SENDS_NODE_MANIFEST, message);
				try {
					// System.out.println("Sending manifest to node: " + client.toString());
					connectionCache.getConnection(key).sendData(e.getBytes());
				} catch (IOException e1) {
					System.out.println("Error sending manifest to client " + key + ": ");
					e1.printStackTrace();
				}
			}

			System.out.println("Overlay setup complete with NR " + NR);

		}else{
			System.out.println("Unable to setup overlay, you have " + nodeRegistered.size() + " node(s) registerted, need a minimum of two.");
		}

	}

	/******************************************
	 ************* END Event Types ************
	 ******************************************/

	/**
	 * Prints list of connected Nodes
	 * @return void
	 */
	public void printNodes(){

		/*
		 * Print out registered nodes
		 */

		if(nodeRegistered.isEmpty()){
			System.out.println("There are currently no registered nodes.");
		}else{
			System.out.println("Currently registered nodes:");

			for (Entry<Integer, OverlayNodeSendsRegistration> entry : nodeRegistered.entrySet()) {
				System.out.println("NodeID: " + entry.getKey() + ", " + entry.getValue());
			}
		}

	}

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

	/**
	 * Print each nodes routing table
	 * Called by the command parser
	 */
	public void listRoutingTables(){
		if(routingTable != null){
			System.out.println(routingTable.getRoutingTables());
		}else{
			System.out.println("Routing table not yet setup. Please issue the \"setup-overlay [num-routing-table-entries]\" command first.");
		}
	}

	/**
	 * Close connections and shut down!
	 */
	public void shutDown(){
		// Not yet implemented...
	}
	
	/**
	 * Clear the routing table
	 */
	private void resetRoutingTable(){
		if(routingTable != null)
			routingTable.clear();
	}

}// ************** END Registry class **************
