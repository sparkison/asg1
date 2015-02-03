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


@SuppressWarnings("unused")
public abstract class TCPServer implements Runnable{

    // Instance variables **************
    private ServerSocket serverSocket = null;
    private Thread connectionListener;
    private ThreadGroup clientThreadGroup;
    private int port;
    private int backlog = 1000;
    //private ThreadGroup clientThreadGroup;
    private boolean iAmListening = true;
    private EventFactory ef = EventFactory.getInstance();

    // Constructor **************
    public TCPServer(int port){
        this.port = port;

        this.clientThreadGroup = new ThreadGroup("TCPConnectionThread threads"){
            public void uncaughtException(Thread thread, Throwable exception){
                clientException((TCPConnectionThread)thread, exception);
            }
        };
    }

    /**
     * Starts listening for connections
     * on the server socket
     * @throws IOException
     * @return void
     */
    final public void listen() throws IOException{
        if (!isListening())
        {
            if (serverSocket == null)
            {
                serverSocket = new ServerSocket(getPort(), backlog);
                // Need to see if auto-assigning port or not...
                if(getPort() == 0){
                    this.port = serverSocket.getLocalPort();
                }
            }

            iAmListening = true;
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
     * Close the server down
     * @throws IOException
     * @return void
     */
    final synchronized public void close() throws IOException{
        if (serverSocket == null) return;

        stopListening();

        try
        {
            serverSocket.close();
        }
        finally
        {
            // Shut it down!
            // Close sockets to connected clients...
            Thread[] clientThreadList = getConnectedClients();

            for(Thread client : clientThreadList){
                try
                {
                    ((TCPConnectionThread)client).close();
                }
                // Ignore all exceptions when closing clients.
                catch(Exception exc) {
                    System.out.println("Error closing client connection: ");
                    exc.printStackTrace();
                }
            }

            serverSocket = null;
            serverClosed();
        }
    }

    /**
     * Tells whether the server thread is listening
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
        Thread[] clientThreadList = new
                Thread[clientThreadGroup.activeCount()];

        clientThreadGroup.enumerate(clientThreadList);

        return clientThreadList;
    }

    /**
     * A count of how many clients are connected
     * to this TCPServer
     * @return int
     */
    final public int getNumberOfClients(){
        return clientThreadGroup.activeCount();
    }

    /**
     * GETTERS **************
     */
    
    final public ThreadGroup getThreadGroup(){
        return clientThreadGroup;
    }

    final public int getPort(){
        return port;
    }

    final public InetAddress getInetAddress() {
        return serverSocket.getInetAddress();
    }

    /**
     * Used for the start() call to run this thread
     */
    final public void run(){
        // call the hook method to notify that the server is starting
        serverStarted();

        try
        {
            // Repeatedly waits for a new client connection, accepts it, and
            // starts a new thread to handle data exchange.
            while(iAmListening)
            {
                try
                {
                    // Wait here for new connection attempts, or a timeout
                    Socket clientSocket = serverSocket.accept();

                    // When a client is accepted, create a thread to handle
                    // the data exchange, then add it to thread group

                    synchronized(this)
                    {
                        TCPConnectionThread c = new TCPConnectionThread(this.clientThreadGroup, clientSocket, this);
                    }
                }
                catch (InterruptedIOException exception)
                {
                    // This will be thrown when a timeout occurs.
                    // No need to throw exception here.
                }
            }

            // call the hook method to notify that the server has stopped
            serverStopped();
        }
        catch (IOException exception)
        {
            if (iAmListening)
            {
                // Closing the socket must have thrown a SocketException
                listeningException(exception);
            }
            else
            {
                serverStopped();
            }
        }
        finally
        {
            iAmListening = false;
            connectionListener = null;
        }
    }

    /**
     * Unimplemented methods
     * Placeholders for debugging
     */

    protected void clientConnected(TCPConnectionThread client) {}

    synchronized protected void clientDisconnected(TCPConnectionThread client) {}

    synchronized protected void clientException(TCPConnectionThread client, Throwable exception) {}

    protected void listeningException(Throwable exception) {}

    protected void serverStarted() {}

    protected void serverStopped() {}

    protected void serverClosed() {}

    /**
     * Method to inherit by sub-class
     * Used to handle messages
     * @param data
     * @param client
     */
    protected abstract void onEvent(Event event, TCPConnectionThread client);

    final synchronized void receiveMessageFromClient(byte[] data, TCPConnectionThread client){
    	Event event = ef.getEvent(data);
        onEvent(event, client);
    }

}// ************** END TCPServer class **************