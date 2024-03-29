/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.transport;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.EventFactory;


public abstract class TCPServer implements Runnable{

	// Instance variables **************
	private EventFactory ef = EventFactory.getInstance();
	private ServerSocket serverSocket = null;
	private Thread connectionListener;
	private ThreadGroup clientThreadGroup;
	private Object addClientLock = new Object();
	private boolean iAmListening = true;
	private int port;

	// Constructor **************
	public TCPServer(int port, String threadGroup){
		// Set the port
		this.port = port;
		// A ThreadGroup to hold the clients connected to this registry
		this.clientThreadGroup = new ThreadGroup(threadGroup);
	}

	/**
	 * Starts listening for connections
	 * on the registry socket
	 * @throws IOException
	 * @return void
	 */
	final public void listen() throws IOException{
		if (!isListening()){
			if (serverSocket == null){
				serverSocket = new ServerSocket(getPort());
				// Need to see if auto-assigning port or not...
				if(getPort() == 0){
					this.port = serverSocket.getLocalPort();
				}
			}
			iAmListening = true;
			// Spawn a thread for self, and start it
			connectionListener = new Thread(this);
			connectionListener.start();
		}
	}

	/**
	 * Call method to stop listening for connections
	 * on this TCPServer instance
	 */
	final public void stopListening(){
		iAmListening = false;
	}

	/**
	 * Tells whether the registry thread is listening
	 * @return boolean
	 */
	final public boolean isListening(){
		return (connectionListener != null);
	}

	/**
	 * Returns list of connected clients
	 * List is Threads[] array of TCPConnectionThread objects
	 * @return Thread[]
	 */
	synchronized final public Thread[] getConnectedClients(){
		Thread[] clientThreadList = new Thread[clientThreadGroup.activeCount()];
		clientThreadGroup.enumerate(clientThreadList);
		return clientThreadList;
	}

	/**
	 * GETTERS **************
	 */

	final public int getNumberOfClients(){return clientThreadGroup.activeCount();}	

	final public ThreadGroup getThreadGroup(){return clientThreadGroup;}

	final public int getPort(){return port;}

	final public InetAddress getInetAddress(){return serverSocket.getInetAddress();}

	/**
	 * Used for the start() call to run this thread
	 */
	final public void run(){
		// call the hook method to notify that the registry is starting
		registryHasStarted();
		try{
			// Repeatedly waits for a new client connection, accepts it, and
			// starts a new thread to handle data exchange.
			while(iAmListening){
				try{
					// Wait here for new connection attempts, or a timeout
					Socket clientSocket = serverSocket.accept();
					// When a client is accepted, create a thread to handle
					// the data exchange, then add it to thread group
					synchronized(addClientLock){
						new TCPConnectionThread(this.clientThreadGroup, clientSocket, this);
					}
				}
				catch (InterruptedIOException exception){} // Called when timeout occurs, not used for now.
			}
			// call the hook method to notify that the registry has stopped
			registryHasStopped();
		}catch (IOException exception){
			if (iAmListening){
				// Closing the socket must have thrown a SocketException
				listeningException(exception);
			}else{
				registryHasStopped();
			}
		}finally{
			iAmListening = false;
			connectionListener = null;
		}
	}

	/**
	 * Close the registry down
	 * @throws IOException
	 * @return void
	 */
	final synchronized public void close() throws IOException{
		if (serverSocket != null) {
			stopListening();

			try{
				serverSocket.close();
			}
			finally{
				// Shut it down!
				// Close sockets to connected clients...
				Thread[] clientThreadList = getConnectedClients();

				for(Thread client : clientThreadList){
					try{
						((TCPConnectionThread)client).close();
					}catch(Exception exc) {// Ignore all exceptions when closing clients.
						System.out.println("Error closing client connection: ");
						exc.printStackTrace();
					}
				}
				serverSocket = null;
				registryHasClosed();
			}
		}

	}
	
	/**
	 * HOOK methods for debugging
	 */

	protected void clientConnected(TCPConnectionThread client) {}

	synchronized protected void clientDisconnected(TCPConnectionThread client) {}

	synchronized protected void clientException(TCPConnectionThread client, Throwable exception) {}

	protected void listeningException(Throwable exception) {}

	protected void registryHasStarted() {}

	protected void registryHasStopped() {}

	protected void registryHasClosed() {}

	/**
	 * Method to inherit by sub-class
	 * Used to handle messages
	 * @param data
	 * @param client
	 */
	protected abstract void onEvent(Event event, TCPConnectionThread client);

	final synchronized void messageFromClient(byte[] data, TCPConnectionThread client){
		Event event = ef.getEvent(data);
		// passing TCPConnectionThread on to the Registry to 
		// make things easier for response messages
		onEvent(event, client);
	}

}// ************** END TCPServer class **************