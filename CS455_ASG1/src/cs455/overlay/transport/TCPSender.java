package cs455.overlay.transport;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class TCPSender {
	@SuppressWarnings("unused")
	private Socket socket;
	private DataOutputStream dout;
	private ReentrantLock sendLock;

	public TCPSender(Socket socket) throws IOException {
		this.socket = socket;
		sendLock = new ReentrantLock();
		dout = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	}
	
	public void sendData(byte[] dataToSend) throws IOException {
		try {
	        sendLock.lock();
	        int dataLength = dataToSend.length;
	        dout.writeInt(dataLength);
	        dout.write(dataToSend, 0, dataLength);
	        dout.flush();
	    }
	    finally {
	         sendLock.unlock();
	    }
		
	}
}