package cs455.overlay.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class OverlayNodeSendsData implements Event{

	private int type;
	private int destinationID;
	private int sourceID;
	private int payLoad;
	private int hopTraceLength = 0;
	private String hopTrace;

	public OverlayNodeSendsData(int type, int destinationID, int sourceID, int payLoad, int hopTraceLength, String hopTrace){
		this.type = type;
		this.destinationID = destinationID;
		this.sourceID = sourceID;
		this.payLoad = payLoad;
		this.hopTraceLength = hopTraceLength;
		this.hopTrace = hopTrace;
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
			dout.writeInt(destinationID);
			dout.writeInt(sourceID);
			dout.writeInt(payLoad);
			dout.writeInt(hopTraceLength);
			
			byte[] hopTraceBytes = hopTrace.getBytes();
			int elementLength = hopTraceBytes.length;
			dout.writeInt(elementLength);
			dout.write(hopTraceBytes);
			
			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();

		} catch (IOException e) {
			System.out.println("Error marshalling the bytes for OverlayNodeSendsData.");
			e.printStackTrace();
		}

		return marshalledBytes;

	}

	// Unmarshalling (unpack the bytes)
	public OverlayNodeSendsData(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();
		destinationID = din.readInt();
		sourceID = din.readInt();
		payLoad = din.readInt();
		hopTraceLength = din.readInt();
		
		int identifierLength = din.readInt();
		byte[] identifierBytes = new byte[identifierLength];
		din.readFully(identifierBytes);
		hopTrace = new String(identifierBytes);

		baInputStream.close();
		din.close();
	}

	@Override
	public int getType() {
		return type;
	}

	/**
	 * @return the destinationID
	 */
	public int getDestinationID() {
		return destinationID;
	}

	/**
	 * @return the sourceID
	 */
	public int getSourceID() {
		return sourceID;
	}

	/**
	 * @return the payLoad
	 */
	public int getPayLoad() {
		return payLoad;
	}

	/**
	 * @return the hopTraceLength
	 */
	public int getHopTraceLength() {
		return hopTraceLength;
	}

	/**
	 * @return the hopTrace
	 */
	public String getHopTrace() {
		return hopTrace;
	}
	
	public synchronized void updateHopTrace(int hop){
		if(hopTrace.equals(" "))
			hopTrace = "" + hop;
		else
			hopTrace += ("->" + hop);
	}
	
	public synchronized void updateHopLength(){
		hopTraceLength += 1;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "OverlayNodeSendsData [type="
				+ type
				+ ", destinationID="
				+ destinationID
				+ ", sourceID="
				+ sourceID
				+ ", payLoad="
				+ payLoad
				+ ", hopTraceLength="
				+ hopTraceLength
				+ ", "
				+ (hopTrace != null ? "hopTrace=" + hopTrace
						: "") + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + destinationID;
		result = prime * result + payLoad;
		result = prime * result + sourceID;
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
		if (!(obj instanceof OverlayNodeSendsData)) {
			return false;
		}
		OverlayNodeSendsData other = (OverlayNodeSendsData) obj;
		if (destinationID != other.destinationID) {
			return false;
		}
		if (payLoad != other.payLoad) {
			return false;
		}
		if (sourceID != other.sourceID) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

}
