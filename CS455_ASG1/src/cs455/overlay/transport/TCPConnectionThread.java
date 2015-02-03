/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.transport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

@SuppressWarnings("rawtypes")
public class TCPConnectionThread extends Thread {

	// Instance variables **************
	private TCPServer server;
	private Socket clientSocket;
	private DataInputStream din;
	private DataOutputStream dout;
	private boolean iAmListening = false;
	private HashMap savedInfo = new HashMap(5);

	// Constructor **************
	public TCPConnectionThread(ThreadGroup group, Socket clientSocket, TCPServer server) throws IOException {
		super(group, (Runnable) null);
		// Initialize variables
		this.clientSocket = clientSocket;
		this.server = server;

		// Initialize our din/dout streams
		try {
			dout = new DataOutputStream(clientSocket.getOutputStream());
			din = new DataInputStream(clientSocket.getInputStream());
		}catch (IOException ex){
			try{
				closeAll();
			}catch (Exception exc) {
				System.out.println("Error setting up data streams: ");
				exc.printStackTrace();
			}
			// Throw exception up to implementing class
			throw ex; 
		}

		iAmListening = true;
		// Start the thread and wait for data from the socket
		start(); 
	}

	/**
	 * Sends data to this client
	 * @param data
	 * @throws IOException
	 * @return void
	 */
	final public void sendToClient(byte[] data) throws IOException{
		if (clientSocket == null || dout == null)
			throw new SocketException("socket does not exist");

		int dataLength = data.length;
		dout.writeInt(dataLength);
		dout.write(data, 0, dataLength);
		dout.flush();
	}

	/**
	 * Close this socket
	 * @throws IOException
	 */
	final public void close() throws IOException{
		iAmListening = false; // Tells the thread to stop

		try{
			closeAll();
		}finally{
			// Hook for TCPServer to let it know we've disconnected 
			server.clientDisconnected(this);
		}
	}

	/**
	 * For setting extra data relevant to this client
	 * Idea: use to save packet sent, received, etc.
	 * @param String infoType
	 * @param Object info
	 */
	@SuppressWarnings("unchecked")
	public void setInfo(String infoType, Object info){
		savedInfo.put(infoType, info);
	}

	/**
	 * For getting extra data relevant to this client
	 * @param String infoType
	 */
	public Object getInfo(String infoType){
		return savedInfo.get(infoType);
	}

	/**
	 * Get the InetAddress of this socket
	 * @return
	 */
	final public InetAddress getInetAddress(){
		return clientSocket == null ? null : clientSocket.getInetAddress();
	}

	public String toString(){
		return clientSocket == null ? null : clientSocket.getInetAddress().getHostName() + " ("
				+ clientSocket.getInetAddress().getHostAddress() + ") " + clientSocket.getPort();
	}

	/**
	 * Used for the start() call to run this thread
	 * Don't call directly. start() is called by constructor
	 */
	final public void run(){
		// Tell the server we connected
		server.clientConnected(this);

		// Read the din stream and respond to messages
		try{
			// The message from the client
			int dataLength;
			byte[] data;

			while (iAmListening){
				// This block waits until it reads a message from the client
				// and then sends it for handling by the server
				dataLength = din.readInt();
				data = new byte[dataLength];
				din.readFully(data, 0, dataLength);
				server.receiveMessageFromClient(data, this);
			}
		}catch (Exception exception){
			if (iAmListening){
				try{
					closeAll();
				} catch (Exception exc){
					System.out.println("");
					exc.printStackTrace();
				}
				server.clientException(this, exception);
			}
		}
	}

	/**
	 * Close it down!
	 * @throws IOException
	 */
	private void closeAll() throws IOException{
		try {
			// Close the socket
			if (clientSocket != null)
				clientSocket.close();
			if (dout != null)
				dout.close();
			if (din != null)
				din.close();
		}finally{
			dout = null;
			din = null;
			clientSocket = null;
		}
	}

	protected void finalize(){
		try {
			closeAll();
		} catch (IOException exc){
			System.out.println("");
			exc.printStackTrace();
		}
	}

}// ************** END TCPConnection class **************