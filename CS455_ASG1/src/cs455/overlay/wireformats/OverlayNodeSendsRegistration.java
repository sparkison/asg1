package cs455.overlay.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeSendsRegistration implements Event{

	// Local vars
	private int type;
	private int ipAddressLength;
	private String ipAddress;
	private int portNum;

	public OverlayNodeSendsRegistration(int type, int ipAddressLength, String ipAddress, int portNum){
		this.type = type;
		this.ipAddressLength = ipAddressLength;
		this.ipAddress = ipAddress;
		this.portNum = portNum;
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
			dout.writeInt(ipAddressLength);

			byte[] ipAddressBytes = ipAddress.getBytes();
			int elementLength = ipAddressBytes.length;
			dout.writeInt(elementLength);
			dout.write(ipAddressBytes);

			dout.writeInt(portNum);

			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();
			
		} catch (IOException e) {
			System.out.println("Error marshalling the bytes for OverlayNodeSendsRegistration.");
			e.printStackTrace();
		}
		
		return marshalledBytes;

	}

	// Unmarshalling (unpack the bytes)
	public OverlayNodeSendsRegistration(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();
		ipAddressLength = din.readInt();

		int identifierLength = din.readInt();
		byte[] identifierBytes = new byte[identifierLength];
		din.readFully(identifierBytes);
		ipAddress = new String(identifierBytes);

		portNum = din.readInt();

		baInputStream.close();
		din.close();
	}

	@Override
	public int getType() {
		return type;
	}

	/**
	 * @return the ipAddressLength
	 */
	public int getipAddressLength() {
		return ipAddressLength;
	}

	/**
	 * @return the ipAddress
	 */
	public String getipAddress() {
		return ipAddress;
	}

	/**
	 * @return the portNum
	 */
	public int getPortNum() {
		return portNum;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "IP Address: " + ipAddress + ", Port Number: " + portNum;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
		result = prime * result + ipAddressLength;
		result = prime * result + portNum;
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
		if (!(obj instanceof OverlayNodeSendsRegistration)) {
			return false;
		}
		OverlayNodeSendsRegistration other = (OverlayNodeSendsRegistration) obj;
		if (ipAddress == null) {
			if (other.ipAddress != null) {
				return false;
			}
		} else if (!ipAddress.equals(other.ipAddress)) {
			return false;
		}
		if (ipAddressLength != other.ipAddressLength) {
			return false;
		}
		if (portNum != other.portNum) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

}