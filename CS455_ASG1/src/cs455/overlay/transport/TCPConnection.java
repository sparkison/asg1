package cs455.overlay.transport;

import java.io.IOException;
import java.net.Socket;

import cs455.overlay.node.Node;
import cs455.overlay.wireformats.EventFactory;

public class TCPConnection{

	private EventFactory ef = EventFactory.getInstance();
	private TCPSender sender;
	private TCPReceiverThread receiver;
	private Socket socket;
	private int connectionId;
	
	public TCPConnection(int id, Socket socket, Node node){
		
		this.connectionId = id;
		this.socket = socket;
		
		try {
			
			/*
			 * Passing the connection id onto the receiver
			 * Whenever we receive data from this connection, we
			 * can determine where it came from
			 */
			this.receiver = new TCPReceiverThread(id, socket, node);
			Thread receive = new Thread(receiver);
			receive.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.sender = new TCPSender(socket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public Socket getSocket(){
		return socket;
	}
	
	public void sendData(byte[] data) throws IOException{
		sender.sendData(data);
	}
	
	/**
	 * Get the id assigned to this connection
	 * @return int
	 */
	public int getConnectionId(){
		return connectionId;
	}

}
