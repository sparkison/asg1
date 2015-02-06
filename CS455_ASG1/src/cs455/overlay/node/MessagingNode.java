package cs455.overlay.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cs455.overlay.routing.RoutingEntry;
import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPReceiver;
import cs455.overlay.transport.TCPSender;
import cs455.overlay.util.InteractiveCommandParser;
import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.EventFactory;
import cs455.overlay.wireformats.Protocol;
import cs455.overlay.wireformats.RegistryReportsRegistrationStatus;

public class MessagingNode implements Node{

	// Instance variables **************
	private Map<Integer, TCPConnection> clientConnections = new HashMap<Integer, TCPConnection>();
	private EventFactory ef = EventFactory.getInstance();
	private String myIPAddress;
	private int myID;
	private int port;
	private int listenPort;
	private String host;
	private Socket clientSocket;
	private ServerSocket svSocket;
	private TCPSender sender;
	private TCPReceiver receiver;
	private List<RoutingEntry> routingTable;
	private int[] nodeList;
	private int sendTracker;
	private long sendSummation;


	public MessagingNode(String host, int port){
		this.host = host;
		this.port = port;
		try {

			// Setup connections (input/output streams)
			this.clientSocket = new Socket(host, port);
			this.sender = new TCPSender(clientSocket);
			this.receiver = new TCPReceiver(clientSocket, this);
			Thread receive = new Thread(receiver);
			receive.start();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			// Open ServerSocket to accept data from other Messaging Nodes
			this.svSocket = new ServerSocket(0);
			// Set listening port to pass to Registry
			this.listenPort = svSocket.getLocalPort();
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
			sender.sendData(registerClient.getBytes());
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
						TCPConnection clientConnection = new TCPConnection(client, messageNode);

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});  
		listener.start();
	}

	/**
	 * Method to test connection to Registry
	 */
	public void testSends(){
		String test = "test message!";
		try {
			sender.sendData(test.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onEvent(Event event) {
		switch (event.getType()){

		case Protocol.REGISTRY_REPORTS_REGISTRATION_STATUS:
			registerStatusEvent(event);
			break;

		case Protocol.REGISTRY_REPORTS_DEREGISTRATION_STATUS:
			//deregisterStatusEvent(event);
			break;

		case Protocol.REGISTRY_SENDS_NODE_MANIFEST:
			//setupRoutingTable(event);
			break;

		case Protocol.REGISTRY_REQUESTS_TASK_INITIATE:
			//startTask(event);
			break;

		case Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY:
			//getStats();
			break;

		default:
			System.out.println("Unrecognized event type received");

		}
	}// END onEvent **************

	/******************************************
	 *************** Event Types **************
	 ******************************************/

	// Get registration status
	// Called within class
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
			System.exit(-1);
		}

	}

	/*
	 * SEND TO SERVER
	 */

	// Setup deRegistration message
	// Called by CommandParser, need to be public
	public void sendDeregistration(){
		String message = myIPAddress.length() + ";" + myIPAddress + ";" + listenPort + ";" + myID;
		Event e = ef.buildEvent(Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION, message);
		try {
			sender.sendData(e.getBytes());
		} catch (IOException e1) {
			System.out.println("Error sending deregistration to Registry: ");
			e1.printStackTrace();
		}

	}

	/******************************************
	 ************* END Event Types ************
	 ******************************************/


}
