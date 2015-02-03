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

import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.EventFactory;


public abstract class TCPClient implements Runnable{

	// Instance variables **************
	private EventFactory ef = EventFactory.getInstance();
	private DataOutputStream dout;
	private DataInputStream din;
	private Socket clientSocket;
	private Thread clientThread;
	private String host;
	private boolean iAmListening = false;
	private int port;

	// Constructor **************
	public TCPClient(String host, int port){
		// Set the host and port, call openConnection() to setup connection
		this.host = host;
		this.port = port;
	}

	/**
	 * Create connection to the server
	 * @throws IOException
	 */
	final public void openConnection() throws IOException{
		// Do not do anything if the connection is already open
		if (!isConnected()){
			// Initialize our data input/output streams, and socket
			try{
				clientSocket = new Socket(host, port);
				dout = new DataOutputStream(clientSocket.getOutputStream());
				din = new DataInputStream(clientSocket.getInputStream());
			}catch (IOException ex){
				try{
					closeAll();
				}catch (Exception exc){
					System.out.println("Error setting up connection with Registry: ");
					exc.printStackTrace();
				}
				// Throw exception up to implementing class
				throw ex; 
			}
			// Create the thread to handle receiving of data
			clientThread = new Thread(this); 
			// Used for the loop to keep listening
			iAmListening = true;
			// Start it up!!
			clientThread.start();
		}

	}

	/**
	 * Send message to server
	 * @param data
	 * @throws IOException
	 */
	final public void sendToServer(byte[] data) throws IOException{
		if (clientSocket == null || dout == null)
			throw new SocketException("socket does not exist");

		int dataLength = data.length;
		dout.writeInt(dataLength);
		dout.write(data, 0, dataLength);
		dout.flush();

	}

	/**
	 * Shut it down!
	 * @throws IOException
	 */
	final public void closeConnection() throws IOException{
		// Tell the thread to stop listening
		iAmListening = false; 

		try{
			closeAll();
		}finally{
			// Call hook method to signal connection closed
			connectionClosed(); 
		}
		
	}

	/**
	 * Tells whether the server thread is listening
	 * @return boolean
	 */
	final public boolean isConnected(){
		return clientThread != null && clientThread.isAlive();
	}

	/**
	 * GETTERS **************
	 */

	final public int getLocalPort(){
		return clientSocket.getLocalPort();
	}

	final public int getServerPort(){
		return port;
	}

	final public String getHost(){
		return host;
	}

	final public InetAddress getInetAddress(){
		return clientSocket.getInetAddress();
	}

	/**
	 * Used for the start() call to run this thread
	 */
	final public void run(){
		connectionEstablished();

		// The message from the server
		byte[] data;
		int dataLength;

		try{
			while (iAmListening){
				// System.out.println("Data received from server");
				dataLength = din.readInt();
				data = new byte[dataLength];
				din.readFully(data, 0, dataLength);
				Event event = ef.getEvent(data);
				onEvent(event);
			}
		}catch (Exception exception){
			if (iAmListening){
				try {
					closeAll();
				} catch (Exception exc) {
					System.out.println("Error reading din stream: ");
					exc.printStackTrace();
				}

				connectionException(exception);
			}
		}finally{
			clientThread = null;
		}
	}

	/**
	 * HOOK methods for debugging
	 */

	protected void connectionClosed() {}

	protected void connectionException(Exception exception) {}

	protected void connectionEstablished() {}

	/**
	 * Method to inherit by sub-class
	 * Used to handle messages from Registry
	 * @param Event
	 */
	protected abstract void onEvent(Event event);


	private void closeAll() throws IOException{
		try{
			// Close the socket
			if (clientSocket != null)
				clientSocket.close();
			if (dout != null)
				dout.close();
			if (din != null)
				din.close();
		} finally {
			dout = null;
			din = null;
			clientSocket = null;
		}
	}

}// ************** END TCPClient class **************