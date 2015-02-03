package cs455.overlay.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeReportsTrafficSummary implements Event{

	private int type;
	private int nodeID;
	private int numPacketsSent;
	private int numPacketsRelayed;
	private long sumPacketDataSent;
	private int numPacketsReceived;
	private long sumPacketDataReceived;

	public OverlayNodeReportsTrafficSummary(int type, int nodeID, int numPacketsSent, int numPacketsRelayed, 
			long sumPacketDataSent, int numPacketsReceived, long sumPacketDataReceived){
		this.type = type;
		this.nodeID = nodeID;
		this.numPacketsSent = numPacketsSent;
		this.numPacketsRelayed = numPacketsRelayed;
		this.sumPacketDataSent = sumPacketDataSent;
		this.numPacketsReceived = numPacketsReceived;
		this.sumPacketDataReceived = sumPacketDataReceived;
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
			dout.writeInt(nodeID);
			dout.writeInt(numPacketsSent);
			dout.writeInt(numPacketsRelayed);
			dout.writeLong(sumPacketDataSent);
			dout.writeInt(numPacketsReceived);
			dout.writeLong(sumPacketDataReceived);

			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();

		} catch (IOException e) {
			System.out.println("Error marshalling the bytes for OverlayNodeReportsTrafficSummary.");
			e.printStackTrace();
		}

		return marshalledBytes;

	}

	// Unmarshalling (unpack the bytes)
	public OverlayNodeReportsTrafficSummary(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();
		nodeID = din.readInt();
		numPacketsSent = din.readInt();
		numPacketsRelayed = din.readInt();
		sumPacketDataSent = din.readLong();
		numPacketsReceived = din.readInt();
		sumPacketDataReceived = din.readLong();
		

		baInputStream.close();
		din.close();
	}

	@Override
	public int getType() {
		return type;
	}

	/**
	 * @return the nodeID
	 */
	public int getNodeID() {
		return nodeID;
	}

	/**
	 * @return the numPacketsSent
	 */
	public int getNumPacketsSent() {
		return numPacketsSent;
	}

	/**
	 * @return the numPacketsRelayed
	 */
	public int getNumPacketsRelayed() {
		return numPacketsRelayed;
	}

	/**
	 * @return the sumPacketDataSent
	 */
	public long getSumPacketDataSent() {
		return sumPacketDataSent;
	}

	/**
	 * @return the numPacketsReceived
	 */
	public int getNumPacketsReceived() {
		return numPacketsReceived;
	}

	/**
	 * @return the sumPacketDataReceived
	 */
	public long getSumPacketDataReceived() {
		return sumPacketDataReceived;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "OverlayNodeReportsTrafficSummary [type=" + type + ", nodeID="
				+ nodeID + ", numPacketsSent=" + numPacketsSent
				+ ", numPacketsRelayed=" + numPacketsRelayed
				+ ", sumPacketDataSent=" + sumPacketDataSent
				+ ", numPacketsReceived=" + numPacketsReceived
				+ ", sumPacketDataReceived=" + sumPacketDataReceived + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + nodeID;
		result = prime * result + numPacketsReceived;
		result = prime * result + numPacketsRelayed;
		result = prime * result + numPacketsSent;
		result = prime
				* result
				+ (int) (sumPacketDataReceived ^ (sumPacketDataReceived >>> 32));
		result = prime * result
				+ (int) (sumPacketDataSent ^ (sumPacketDataSent >>> 32));
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
		if (!(obj instanceof OverlayNodeReportsTrafficSummary)) {
			return false;
		}
		OverlayNodeReportsTrafficSummary other = (OverlayNodeReportsTrafficSummary) obj;
		if (nodeID != other.nodeID) {
			return false;
		}
		if (numPacketsReceived != other.numPacketsReceived) {
			return false;
		}
		if (numPacketsRelayed != other.numPacketsRelayed) {
			return false;
		}
		if (numPacketsSent != other.numPacketsSent) {
			return false;
		}
		if (sumPacketDataReceived != other.sumPacketDataReceived) {
			return false;
		}
		if (sumPacketDataSent != other.sumPacketDataSent) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

}
