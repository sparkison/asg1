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


public abstract class TCPClient implements Runnable{

    // Instance variables **************
    private Socket clientSocket;
    private DataOutputStream output;
    private DataInputStream input;
    private Thread clientReader;
    private boolean readyToStop = false;
    private String host;
    private int port;

    // Constructor **************
    public TCPClient(String host, int port) {
        // Initialize variables
        this.host = host;
        this.port = port;
    }

    /**
     * Create connection to the server
     * @throws IOException
     */
    final public void openConnection() throws IOException {
        // Do not do anything if the connection is already open
        if (isConnected())
            return;

        // Create the sockets and the input/output streams
        try {
            clientSocket = new Socket(host, port);
            output = new DataOutputStream(clientSocket.getOutputStream());
            input = new DataInputStream(clientSocket.getInputStream());
        } catch (IOException ex){
            try {
                closeAll();
            } catch (Exception exc) {
                System.out.println("Error setting up connection with Registry: ");
                exc.printStackTrace();
            }

            throw ex; // Re-throw the exception.
        }

        clientReader = new Thread(this); // Create the data reader thread
        readyToStop = false;
        clientReader.start(); // Start the thread
    }

    /**
     * Send message to server
     * @param data
     * @throws IOException
     */
    final public void sendToServer(byte[] data) throws IOException {
        if (clientSocket == null || output == null)
            throw new SocketException("socket does not exist");
        
        int dataLength = data.length;
        output.writeInt(dataLength);
        output.write(data, 0, dataLength);
        output.flush();

    }

    /**
     * Shut it down!
     * @throws IOException
     */
    final public void closeConnection() throws IOException {
        readyToStop = true; // Tells the thread to stop

        try {
            closeAll();
        } finally {
            connectionClosed(); // Call the hook method
        }
    }

    /**
     * Tells whether the server thread is listening
     * @return boolean
     */
    final public boolean isConnected() {
        return clientReader != null && clientReader.isAlive();
    }

    /**
     * GETTERS **************
     */

    final public int getLocalPort() {
        return clientSocket.getLocalPort();
    }

    final public int getServerPort() {
        return port;
    }

    final public String getHost() {
        return host;
    }

    final public InetAddress getInetAddress() {
        return clientSocket.getInetAddress();
    }

    /**
     * Used for the start() call to run this thread
     */
    final public void run() {
        connectionEstablished();

        // The message from the server
        byte[] data;
        int dataLength;

        try {
            while (!readyToStop) {
                // System.out.println("Data received from server");
                dataLength = input.readInt();
                data = new byte[dataLength];
                input.readFully(data, 0, dataLength);
                messageFromServer(data);
            }
        } catch (Exception exception) {
            if (!readyToStop) {
                try {
                    closeAll();
                } catch (Exception exc) {
                    System.out.println("Error reading input stream: ");
                    exc.printStackTrace();
                }

                connectionException(exception);
            }
        } finally {
            clientReader = null;
        }
    }

    /**
     * Unimplemented methods
     * Placeholders for debugging
     */

    protected void connectionClosed() {}

    protected void connectionException(Exception exception) {}

    protected void connectionEstablished() {}

    /**
     * Method to inherit by sub-class
     * Used to handle messages from Registry
     * @param data
     * @param client
     */
    protected abstract void messageFromServer(byte[] data);


    private void closeAll() throws IOException {
        try {
            // Close the socket
            if (clientSocket != null)
                clientSocket.close();

            if (output != null)
                output.close();

            if (input != null)
                input.close();

        } finally {
            output = null;
            input = null;
            clientSocket = null;
        }
    }

}// ************** END TCPClient class **************