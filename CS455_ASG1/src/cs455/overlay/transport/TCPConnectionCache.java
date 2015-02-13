package cs455.overlay.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
		return socket.getLocalAddress();
	}

	public TCPConnection getConnection(int id){
		return registeredNodes.get(id);
	}

	public boolean containsValue(int id){
		return registeredNodes.get(id) == null ? false : true;
	}

	public void sendToAll(byte[] data, String messageInfo){
		for (Integer key : registeredNodes.keySet()) {
			try {
				registeredNodes.get(key).sendData(data);
			} catch (IOException e) {
				System.out.println("Error " + messageInfo + ": ");
				e.printStackTrace();
			}
		}
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
