package cs455.overlay.transport;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import cs455.overlay.node.Node;
import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.EventFactory;

public class TCPReceiver extends Thread{
	private Socket socket;
	private DataInputStream din;
	private Node node;
	private EventFactory ef = EventFactory.getInstance();
	
	public TCPReceiver(Socket socket, Node node) throws IOException {
		this.socket = socket;
		this.node = node;
		din = new DataInputStream(socket.getInputStream());
	}
	public void run() {
		int dataLength;
		while (socket != null) {
			try {
				
				// Get data, and send to node for processing
				dataLength = din.readInt();
				byte[] data = new byte[dataLength];
				din.readFully(data, 0, dataLength);
				// Build Event to send
				Event e = ef.getEvent(data);
				node.onEvent(e);
				
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