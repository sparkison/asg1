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

public class TCPConnectionThread extends Thread {

	// Instance variables **************
	private TCPServer server;
	private Socket clientSocket;
	private DataInputStream din;
	private DataOutputStream dout;
	private Object inputLock = new Object();
	private boolean iAmListening = false;
	private int threadID;

	/*
	 * Methods declared "final" for dealing with concurrent access to Object
	 */

	// Constructor **************
	public TCPConnectionThread(ThreadGroup group, Socket clientSocket, TCPServer server) throws IOException {
		// Initialize variables
		super(group, (Runnable) null);		
		this.clientSocket = clientSocket;
		this.server = server;

		// Initialize data input/output streams
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
	 * Used by the Registry
	 * Doesn't need synchronized b/c only 1 Registry
	 * @param data
	 * @throws IOException
	 * @return void
	 */
	final public void sendFromRegistryToClient(byte[] data) throws IOException{
		if (clientSocket == null || dout == null)
			throw new SocketException("socket does not exist");
		/*
		 * Need to synchronize to avoid concurrency issues
		 * Multiple Threads could be sending messages
		 * to this client at any given time
		 */
		int dataLength = data.length;
		dout.writeInt(dataLength);
		dout.write(data, 0, dataLength);
		dout.flush();
	}

	/**
	 * Sends data to this client
	 * Used by the Client servers
	 * Needs to be synchronized b/c can have multiple
	 * ClientServers sending at the same time
	 * @param data
	 * @throws IOException
	 * @return void
	 */
	final public void sendFromClientToClient(byte[] data) throws IOException{
		if (clientSocket == null || dout == null)
			throw new SocketException("socket does not exist");
		/*
		 * Need to synchronize to avoid concurrency issues
		 * Multiple Threads could be sending messages
		 * to this client at any given time
		 */
		synchronized(inputLock){
			int dataLength = data.length;
			dout.writeInt(dataLength);
			dout.write(data, 0, dataLength);
			dout.flush();
		}
	}

	/**
	 * Close this socket
	 * @throws IOException
	 */
	final public void close() throws IOException{
		// Tell the thread to stop listening for input
		iAmListening = false; 
		try{
			closeAll();
		}finally{
			// Hook for TCPServer to let the Registry know we've disconnected 
			server.clientDisconnected(this);
		}
	}

	/**
	 * For setting extra data relevant to this client
	 * Idea: use to save packet sent, received, etc.
	 * @param String infoType
	 * @param Object info
	 */
	public void setThreadID(int id){
		threadID = id;
	}

	/**
	 * For getting extra data relevant to this client
	 * @param String infoType
	 */
	public int getThreadID(){
		return threadID;
	}

	/**
	 * Get the InetAddress of this socket
	 * @return
	 */
	final public InetAddress getInetAddress(){
		if(clientSocket != null)
			return clientSocket.getInetAddress();
		else 
			return null;

	}

	public String toString(){
		if(clientSocket != null){
			return clientSocket.getInetAddress().getHostName() + " ("
					+ clientSocket.getInetAddress().getHostAddress() + ") " 
					+ clientSocket.getPort();
		}else{
			return null;
		}
	}

	/**
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
				server.messageFromClient(data, this);
			}

		}catch (Exception exception){
			if (iAmListening){
				try{
					closeAll();
				}catch (Exception exc){
					System.out.println("");
					exc.printStackTrace();
				}
				// Hook for TCPServer to let the Registry know we had a problem 
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
		}catch (IOException exc){
			exc.printStackTrace();
		}
	}

}// ************** END TCPConnection class **************