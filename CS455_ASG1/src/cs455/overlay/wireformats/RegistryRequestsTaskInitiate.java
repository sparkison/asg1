package cs455.overlay.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegistryRequestsTaskInitiate implements Event{

	private int type;
	private int numPackets;

	public RegistryRequestsTaskInitiate(int type, int numPackets){
		this.type = type;
		this.numPackets = numPackets;
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
			dout.writeInt(numPackets);

			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();

		} catch (IOException e) {
			System.out.println("Error marshalling the bytes for RegistryRequestsTaskInitiate.");
			e.printStackTrace();
		}

		return marshalledBytes;

	}

	// Unmarshalling (unpack the bytes)
	public RegistryRequestsTaskInitiate(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();
		numPackets = din.readInt();

		baInputStream.close();
		din.close();
	}

	@Override
	public int getType() {
		return type;
	}

	/**
	 * @return the numPackets
	 */
	public int getNumPackets() {
		return numPackets;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RegistryRequestsTaskInitiate [type=" + type + ", numPackets="
				+ numPackets + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + numPackets;
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
		if (!(obj instanceof RegistryRequestsTaskInitiate)) {
			return false;
		}
		RegistryRequestsTaskInitiate other = (RegistryRequestsTaskInitiate) obj;
		if (numPackets != other.numPackets) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

}
