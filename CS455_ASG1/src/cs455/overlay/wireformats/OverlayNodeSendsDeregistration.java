package cs455.overlay.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeSendsDeregistration implements Event{

	private int type;
	private int ipLength;
	private String ipAddress;
	private int portNo;
	private int nodeID;

	public OverlayNodeSendsDeregistration(int type, int ipLength, String ipAddress, int portNo, int nodeID){
		this.type = type;
		this.ipLength = ipLength;
		this.ipAddress = ipAddress;
		this.portNo = portNo;
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
			dout.writeInt(ipLength);

			byte[] ipBytes = ipAddress.getBytes();
			int elementLength = ipBytes.length;
			dout.writeInt(elementLength);
			dout.write(ipBytes);

			dout.writeInt(portNo);
			dout.writeInt(nodeID);

			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();

		} catch (IOException e) {
			System.out.println("Error marshalling the bytes for OverlayNodeSendsDeregistration.");
			e.printStackTrace();
		}

		return marshalledBytes;

	}

	// Unmarshalling (unpack the bytes)
	public OverlayNodeSendsDeregistration(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();
		ipLength = din.readInt();

		int identifierLength = din.readInt();
		byte[] identifierBytes = new byte[identifierLength];
		din.readFully(identifierBytes);
		ipAddress = new String(identifierBytes);

		portNo = din.readInt();
		nodeID = din.readInt();

		baInputStream.close();
		din.close();
	}

	@Override
	public int getType() {
		return type;
	}

	/**
	 * @return the ipLength
	 */
	public int getIpLength() {
		return ipLength;
	}

	/**
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * @return the portNo
	 */
	public int getPortNo() {
		return portNo;
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
		return "OverlayNodeSendsDeregistration [type=" + type + ", ipLength="
				+ ipLength + ", "
				+ (ipAddress != null ? "ipAddress=" + ipAddress + ", " : "")
				+ "portNo=" + portNo + ", nodeID=" + nodeID + "]";
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
		result = prime * result + ipLength;
		result = prime * result + nodeID;
		result = prime * result + portNo;
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
		if (!(obj instanceof OverlayNodeSendsDeregistration)) {
			return false;
		}
		OverlayNodeSendsDeregistration other = (OverlayNodeSendsDeregistration) obj;
		if (ipAddress == null) {
			if (other.ipAddress != null) {
				return false;
			}
		} else if (!ipAddress.equals(other.ipAddress)) {
			return false;
		}
		if (ipLength != other.ipLength) {
			return false;
		}
		if (nodeID != other.nodeID) {
			return false;
		}
		if (portNo != other.portNo) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

}
