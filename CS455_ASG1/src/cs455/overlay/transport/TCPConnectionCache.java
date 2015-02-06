package cs455.overlay.transport;

import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import cs455.overlay.wireformats.OverlayNodeSendsRegistration;

public class TCPConnectionCache {

	// Range of valid ID's for client Nodes (0[inclusive] to 127[exclusive])
	static final int MIN = 0;
	static final int MAX = 127;
	private Map<Integer, TCPConnection> registeredNodes = new HashMap<Integer, TCPConnection>();

	public TCPConnectionCache(){
		// Public constructor
	}

	public void addConnection(int id, TCPConnection connection){
		registeredNodes.put(id, connection);
	}

	public boolean removeConnection(int id){
		return registeredNodes.remove(id) == null ? false : true;
	}

	public InetAddress getInetAddress(int id){
		TCPConnection connection = registeredNodes.get(id);
		Socket socket = connection.getSocket();
		return socket.getInetAddress();
	}
	
	public boolean containsValue(int id){
		return registeredNodes.get(id) == null ? false : true;
	}
	
	public int getNodeId(OverlayNodeSendsRegistration onsr){
		int id = -1;
		for (Entry<Integer, TCPConnection> entry : registeredNodes.entrySet()) {
			
			

		}
		return id;
	}
	
	/**
	 * Get a random nodeID
	 * @return Integer
	 */
	public Integer getNodeID(){

		// Get a random nodeID
		Random rand = new Random();
		Integer n = rand.nextInt(MAX - MIN) + MIN;
		// Make sure we don't have a collision
		while(registeredNodes.containsKey(n)){
			n = rand.nextInt(MAX - MIN) + MIN;
		}
		return n;

	}// END getNodeID **************
}
