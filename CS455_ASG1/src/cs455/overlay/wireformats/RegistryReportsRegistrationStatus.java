package cs455.overlay.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegistryReportsRegistrationStatus implements Event{

	// Local vars
	private int type;
	private int status;
	private int msgLength;
	private String message;

	public RegistryReportsRegistrationStatus(int type, int status, int msgLength, String message){
		this.type = type;
		this.status = status;
		this.msgLength = msgLength;
		this.message = message;
	}

	// Marshalling (packing the bytes)
	@Override
	public byte[] getBytes() {
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(
				baOutputStream));

		try {
			dout.writeInt(type);
			dout.writeInt(status);
			dout.writeInt(msgLength);

			byte[] messageBytes = message.getBytes();
			int elementLength = messageBytes.length;
			dout.writeInt(elementLength);
			dout.write(messageBytes);

			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();

		} catch (IOException e) {
			System.out.println("Error marshalling the bytes for RegistryReportsRegistrationStatus.");
			e.printStackTrace();
		}

		return marshalledBytes;

	}

	// Unmarshalling (unpack the bytes)
	public RegistryReportsRegistrationStatus(byte[] marshalledBytes) throws IOException {
				
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();
		status = din.readInt();
		msgLength = din.readInt();

		int identifierLength = din.readInt();
		byte[] identifierBytes = new byte[identifierLength];
		din.readFully(identifierBytes);
		message = new String(identifierBytes);

		baInputStream.close();
		din.close();
		
	}

	@Override
	public int getType() {
		return type;
	}

	/**
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * @return the msgLength
	 */
	public int getMsgLength() {
		return msgLength;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RegistryReportsRegistrationStatus [type=" + type + ", status="
				+ status + ", msgLength=" + msgLength + ", "
				+ (message != null ? "message=" + message : "") + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + msgLength;
		result = prime * result + status;
		result = prime * result + type;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof RegistryReportsRegistrationStatus)) {
			return false;
		}
		RegistryReportsRegistrationStatus other = (RegistryReportsRegistrationStatus) obj;
		if (message == null) {
			if (other.message != null) {
				return false;
			}
		} else if (!message.equals(other.message)) {
			return false;
		}
		if (msgLength != other.msgLength) {
			return false;
		}
		if (status != other.status) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}


}
