package cs455.overlay.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPSender;
import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.OverlayNodeSendsData;
import cs455.overlay.wireformats.Protocol;

public class MessagingNodeReceiver extends Thread implements Node{

	// Instance variables **************
	private Map<Integer, TCPSender> clientConnections = new HashMap<Integer, TCPSender>();
	private ServerSocket svSocket;
	private int port;
	private int receiveTraker;
	private long receiveSummation;
	private int relayTracker;
	private int myID;
	private boolean debug = false;
	
	
	// MessagingNodeReceiver constructor
	public MessagingNodeReceiver(){
		try {
			// Open ServerSocket to accept data from other Messaging Nodes
			this.svSocket = new ServerSocket(0);
			// Set listening port to pass to Registry
			this.port = svSocket.getLocalPort();
			// Display success message
			System.out.println("Client listening for connections on port: " + port);
		} catch (IOException e) {
			e.printStackTrace();
			close();
		}
	}
	
	/**
	 * Start listening for new connections
	 */
	public void run() {
		while(true){
			try {

				Socket client = svSocket.accept();
				/*
				 * The messaging node doesn't need to assign id's
				 * for record keeping, so defaulting to 0 for connectionID
				 */
				synchronized(this){
					new TCPConnection(0, client, this);
				}							

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/************** TRACKER METHODS **************/
	
	public synchronized void resetCounters(){
		receiveTraker = 0;
		receiveSummation = 0;
		relayTracker = 0;
	}

	private synchronized void updateRelayed(){
		relayTracker++;
	}

	private synchronized void updateReceived(int payload){
		receiveTraker++;
		receiveSummation += payload;
	}
	
	/************ END TRACKER METHODS ************/
	
	@Override
	public void onEvent(Event event, int id) {
		if(event.getType() == Protocol.OVERLAY_NODE_SENDS_DATA){
			dataFromMessageNode(event);
		}else{
			System.out.println("Node received unknown event type.");
		}
	}
	
	/************** DATA ROUTING METHODS **************/
	
	/**
	 * Called when data is sent to this node
	 * from another MessagingNode, this could be
	 * called by multiple other threads at the same time,
	 * need to synchronize access
	 * @param event
	 */
	private synchronized void dataFromMessageNode(Event data){
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
				routeData(sink, data);  				

			}

		} catch (IOException e) {
			System.out.println("Error getting data: ");
			e.printStackTrace();
		}
	}
	
	/**
	 * Method used to route data to nearest neighbor
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
		 * 
		 * Note to self: Is there a better way to do this in a single loop iteration???
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
	
	private void sendDataToClient(int id, Event data){
		
		/************************	DEADLOCK HERE!?!?!?!?	*****************************/
		/****	Getting deadlock on the send method, which uses TCPSender, why??	****/
		
		try {
			clientConnections.get(id).sendData(data.getBytes());
		} catch (IOException e) {
			System.out.println("Error sending payload to client: ");
			e.printStackTrace();
		}
	}
	
	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return the receiveTraker
	 */
	public int getReceiveTraker() {
		return receiveTraker;
	}

	/**
	 * @return the receiveSummation
	 */
	public long getReceiveSummation() {
		return receiveSummation;
	}

	/**
	 * @return the relayTracker
	 */
	public int getRelayTracker() {
		return relayTracker;
	}

	/**
	 * @return the myID
	 */
	public int getMyID() {
		return myID;
	}

	/**
	 * @return the debug
	 */
	public boolean isDebug() {
		return debug;
	}

	/**
	 * @param clientConnections the clientConnections to set
	 */
	public void setClientConnections(Map<Integer, TCPSender> clientConnections) {
		this.clientConnections = clientConnections;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @param myID the myID to set
	 */
	public void setMyID(int myID) {
		this.myID = myID;
	}

	/**
	 * @param debug the debug to set
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/************ END DATA ROUTING METHODS ************/

	@Override
	public void close() {
		try {
			svSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
