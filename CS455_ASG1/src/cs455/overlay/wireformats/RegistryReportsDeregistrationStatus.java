package cs455.overlay.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegistryReportsDeregistrationStatus implements Event{

	private int type;
	private int deregistrationStatus;

	public RegistryReportsDeregistrationStatus(int type, int deregistrationStatus){
		this.type = type;
		this.deregistrationStatus = deregistrationStatus;
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
			dout.writeInt(deregistrationStatus);

			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();

		} catch (IOException e) {
			System.out.println("Error marshalling the bytes for RegistryReportsDeregistrationStatus.");
			e.printStackTrace();
		}

		return marshalledBytes;

	}

	// Unmarshalling (unpack the bytes)
	public RegistryReportsDeregistrationStatus(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();
		deregistrationStatus = din.readInt();

		baInputStream.close();
		din.close();
	}

	@Override
	public int getType() {
		return type;
	}

	/**
	 * @return the deregistrationStatus
	 */
	public int getDeregistrationStatus() {
		return deregistrationStatus;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RegistryReportsDeregistrationStatus [type=" + type
				+ ", deregistrationStatus=" + deregistrationStatus + "]";
	}

}
