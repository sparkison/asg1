package cs455.overlay.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cs455.overlay.routing.RoutingTable;
import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPConnectionCache;
import cs455.overlay.util.InteractiveCommandParser;
import cs455.overlay.util.StatisticsCollectorAndDisplay;
import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.EventFactory;
import cs455.overlay.wireformats.OverlayNodeSendsRegistration;
import cs455.overlay.wireformats.Protocol;

public class Registry implements Node{

	// Instance variables **************
	private StatisticsCollectorAndDisplay statistics = new StatisticsCollectorAndDisplay();
	private EventFactory ef = EventFactory.getInstance();
	private int port;
	private Socket clientSocket;
	private ServerSocket svSocket;
	private TCPConnectionCache connectionCache = new TCPConnectionCache();
	private List<Event> nodesCompleted = new ArrayList<Event>();
	private List<Event> nodesSummary = new ArrayList<Event>();
	private RoutingTable routingTable;
	// Set routing table size (default is 3 if not specified)
	private int NR = 3;


	public Registry(int port){
		this.port = port;
		try {
			// Open ServerSocket to accept data from Messaging Nodes
			svSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
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

						Socket client = svSocket.accept();
						int key = connectionCache.getNodeID();
						TCPConnection clientConnection = new TCPConnection(client, registry);
						connectionCache.addConnection(key, clientConnection);

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
			}
		});
		listener.start();
	}

	@Override
	public void onEvent(Event event) {
		switch (event.getType()){

		case Protocol.OVERLAY_NODE_SENDS_REGISTRATION:
			registerNode(event);
			break;

		case Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION:
			//deRegisterNode(event);
			break;

		case Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS:
			//getSetupStatus(event);
			break;

		case Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED:
			//nodeReportsFinish(event);
			break;

		case Protocol.OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY:
			//nodeReportsSummary(event);
			break;

		default:
			System.out.println("Unrecognized event type received");
		}
	}// END onEvent **************

	/******************************************
	 *************** Event Types **************
	 ******************************************/
	
	private void registerNode(Event event){

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
		String compare = client.getInetAddress().toString().split("/")[1];

		// Get object so we can see the data
		OverlayNodeSendsRegistration clientNode = (OverlayNodeSendsRegistration) event;

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
			client.sendFromRegistryToClient(registrationStatus.getBytes());
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
	
}
