package cs455.overlay.transport;

import java.io.IOException;
import java.net.Socket;

import cs455.overlay.node.Node;
import cs455.overlay.wireformats.EventFactory;

public class TCPConnection{

	private EventFactory ef = EventFactory.getInstance();
	private TCPSender sender;
	private TCPReceiver receiver;
	private Socket socket;
	
	public TCPConnection(Socket socket, Node node){
		
		this.socket = socket;
		
		try {
			
			this.receiver = new TCPReceiver(socket, node);
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
	
	public void sendData(byte[] data){
		try {
			sender.sendData(data);
		} catch (IOException e) {
			System.out.println("Error sending data to server: ");
			e.printStackTrace();
		}
	}

}
