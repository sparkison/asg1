package cs455.overlay.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NodeReportsOverlaySetupStatus implements Event{

	private int type;
	private int status;
	private int infoLength;
	private String information;

	public NodeReportsOverlaySetupStatus(int type, int status, int infoLength, String information){
		this.type = type;
		this.status = status;
		this.infoLength = infoLength;
		this.information = information;
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
			dout.writeInt(infoLength);

			byte[] informationBytes = information.getBytes();
			int elementLength = informationBytes.length;
			dout.writeInt(elementLength);
			dout.write(informationBytes);
			
			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();

		} catch (IOException e) {
			System.out.println("Error marshalling the bytes for NodeReportsOverlaySetupStatus.");
			e.printStackTrace();
		}

		return marshalledBytes;

	}

	// Unmarshalling (unpack the bytes)
	public NodeReportsOverlaySetupStatus(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();
		status = din.readInt();
		infoLength = din.readInt();

		int identifierLength = din.readInt();
		byte[] identifierBytes = new byte[identifierLength];
		din.readFully(identifierBytes);
		information = new String(identifierBytes);
		
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
	 * @return the infoLength
	 */
	public int getInfoLength() {
		return infoLength;
	}

	/**
	 * @return the information
	 */
	public String getInformation() {
		return information;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NodeReportsOverlaySetupStatus [type=" + type + ", status="
				+ status + ", infoLength=" + infoLength + ", "
				+ (information != null ? "information=" + information : "")
				+ "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + infoLength;
		result = prime * result
				+ ((information == null) ? 0 : information.hashCode());
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
		if (!(obj instanceof NodeReportsOverlaySetupStatus)) {
			return false;
		}
		NodeReportsOverlaySetupStatus other = (NodeReportsOverlaySetupStatus) obj;
		if (infoLength != other.infoLength) {
			return false;
		}
		if (information == null) {
			if (other.information != null) {
				return false;
			}
		} else if (!information.equals(other.information)) {
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
