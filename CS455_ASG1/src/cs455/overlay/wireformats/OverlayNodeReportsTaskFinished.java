package cs455.overlay.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeReportsTaskFinished implements Event{
	
	private int type;
	private String ipAddress;
	private int portNum;
	private int nodeID;

	public OverlayNodeReportsTaskFinished(int type, String ipAddress, int portNum, int nodeID){
		this.type = type;
		this.ipAddress = ipAddress;
		this.portNum = portNum;
		this.nodeID = nodeID;
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
			
			byte[] ipAddressBytes = ipAddress.getBytes();
			int elementLength = ipAddressBytes.length;
			dout.writeInt(elementLength);
			dout.write(ipAddressBytes);
			
			dout.writeInt(portNum);
			dout.writeInt(nodeID);

			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();

		} catch (IOException e) {
			System.out.println("Error marshalling the bytes for OverlayNodeReportsTaskFinished.");
			e.printStackTrace();
		}

		return marshalledBytes;

	}

	// Unmarshalling (unpack the bytes)
	public OverlayNodeReportsTaskFinished(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();
		
		int identifierLength = din.readInt();
		byte[] identifierBytes = new byte[identifierLength];
		din.readFully(identifierBytes);
		ipAddress = new String(identifierBytes);

		portNum = din.readInt();
		nodeID = din.readInt();

		baInputStream.close();
		din.close();
	}

	@Override
	public int getType() {
		return type;
	}

	/**
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * @return the portNum
	 */
	public int getPortNum() {
		return portNum;
	}

	/**
	 * @return the nodeID
	 */
	public int getNodeID() {
		return nodeID;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "OverlayNodeReportsTaskFinished [type=" + type + ", "
				+ (ipAddress != null ? "ipAddress=" + ipAddress + ", " : "")
				+ "portNum=" + portNum + ", nodeID=" + nodeID + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((ipAddress == null) ? 0 : ipAddress.hashCode());
		result = prime * result + nodeID;
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
		if (!(obj instanceof OverlayNodeReportsTaskFinished)) {
			return false;
		}
		OverlayNodeReportsTaskFinished other = (OverlayNodeReportsTaskFinished) obj;
		if (ipAddress == null) {
			if (other.ipAddress != null) {
				return false;
			}
		} else if (!ipAddress.equals(other.ipAddress)) {
			return false;
		}
		if (nodeID != other.nodeID) {
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
