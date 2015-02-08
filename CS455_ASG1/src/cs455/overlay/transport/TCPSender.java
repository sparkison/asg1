package cs455.overlay.transport;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPSender {
	@SuppressWarnings("unused")
	private Socket socket;
	private DataOutputStream dout;
	private Object sendLock = new Object();
	
	public TCPSender(Socket socket) throws IOException {
		this.socket = socket;
		dout = new DataOutputStream(socket.getOutputStream());
	}
	public void sendData(byte[] dataToSend) throws IOException {
		/*
		 * Needs to be synchronized as many clients
		 * could be sending data to a given node at a time
		 */
		synchronized(sendLock){
			int dataLength = dataToSend.length;
			dout.writeInt(dataLength);
			dout.write(dataToSend, 0, dataLength);
			dout.flush();
		}
	}
}