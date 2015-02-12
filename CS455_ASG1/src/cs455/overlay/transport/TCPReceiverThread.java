package cs455.overlay.transport;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import cs455.overlay.node.Node;
import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.EventFactory;

public class TCPReceiverThread extends Thread{

	// Instance variables **************
	private Socket socket;
	private DataInputStream din;
	private Node node;
	private EventFactory ef = EventFactory.getInstance();
	private int connectionId;

	/**
	 * Main constructor
	 * @param id
	 * @param socket
	 * @param node
	 * @throws IOException
	 */
	public TCPReceiverThread(int id, Socket socket, Node node) throws IOException {
		this.connectionId = id;
		this.socket = socket;
		this.node = node;
		din = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
	}

	/**
	 * Runs when thread is initialized (.start() is called on it).
	 */
	public void run() {

		while (socket != null) {
			try {

				// Get data, and send to node for processing
				int dataLength = din.readInt();
				byte[] data = new byte[dataLength];
				din.readFully(data, 0, dataLength);
				/*
				 * Build Event to send to receiver
				 * this will be destined for the end
				 * that initiated the ServerSocket.
				 * Passing id with message so we know
				 * where the message originated from
				 */

				Event e = ef.getEvent(data);
				//System.out.println("\nSending: " + e);
				node.onEvent(e, connectionId);	

			} catch (SocketException se) {
				System.out.println(se.getMessage());
				break;
			} catch (IOException ioe) {
				System.out.println(ioe.getMessage()) ;
				break;
			}

		}
	}

}