package cs455.overlay.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cs455.overlay.routing.RoutingEntry;

public class RegistrySendsNodeManifest implements Event{

	private int type;
	private int NR;
	private String routingEntriesString;
	private String allNodesString;
	private int numNodes;

	private List<RoutingEntry> routingEntries = new ArrayList<RoutingEntry>();
	private int[] allNodes;

	public RegistrySendsNodeManifest(int type, int NR, String allNodesString, String routingEntriesString, int numNodes){
		this.type = type;
		this.NR = NR;
		this.allNodesString = allNodesString;
		this.numNodes = numNodes;
		this.routingEntriesString = routingEntriesString;
		
		/*
		 * Get the RoutingEntry list and Nodes list
		 */
		
		//System.out.println(allNodesString);
		//System.out.println(routingEntriesString);
		
		allNodes = getNodeEntries(allNodesString);

		String[] temp = getRoutingEntries(routingEntriesString);
		
		/*
		 * Doing something a little funky (dangerous!?) here
		 * We've already checked to see the length mod 4 = 0
		 * So getting elements four at a time, and incrementing by 4
		 * Reason: know every four is an entry of the format
		 * 1 = id, 2 = ip length, 3 = ip address, and 4 = port number
		 */
		for(int i = 0; i<temp.length; i+=4){
			
			int nodeID = Integer.parseInt(temp[i]);
			int ipLength = Integer.parseInt(temp[i+1]);
			String ipAddress = temp[i+2];
			int portNum = Integer.parseInt(temp[i+3]);
			routingEntries.add(new RoutingEntry(nodeID, ipLength, ipAddress, portNum));
			
		}
		
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
			dout.writeInt(NR);

			byte[] routingEntriesStringBytes = routingEntriesString.getBytes();
			int routingEntriesLength = routingEntriesStringBytes.length;
			dout.writeInt(routingEntriesLength);
			dout.write(routingEntriesStringBytes);

			byte[] allNodesStringBytes = allNodesString.getBytes();
			int allNodesLength = allNodesStringBytes.length;
			dout.writeInt(allNodesLength);
			dout.write(allNodesStringBytes);

			dout.writeInt(numNodes);

			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();

		} catch (IOException e) {
			System.out.println("Error marshalling the bytes for RegistrySendsNodeManifest.");
			e.printStackTrace();
		}

		return marshalledBytes;

	}

	// Unmarshalling (unpack the bytes)
	public RegistrySendsNodeManifest(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();
		NR = din.readInt();

		int routingEntriesLength = din.readInt();
		byte[] routingEntriesBytes = new byte[routingEntriesLength];
		din.readFully(routingEntriesBytes);
		routingEntriesString = new String(routingEntriesBytes);

		int allNodesLength = din.readInt();
		byte[] allNodesBytes = new byte[allNodesLength];
		din.readFully(allNodesBytes);
		allNodesString = new String(allNodesBytes);

		numNodes = din.readInt();

		baInputStream.close();
		din.close();

		/*
		 * Get the RoutingEntry list and Nodes list
		 */
		
		allNodes = getNodeEntries(allNodesString);

		String[] temp = getRoutingEntries(routingEntriesString);
		for(int i = 0; i<temp.length; i+=4){
			
			int nodeID = Integer.parseInt(temp[i]);
			int ipLength = Integer.parseInt(temp[i+1]);
			String ipAddress = temp[i+2];
			int portNum = Integer.parseInt(temp[i+3]);
			
			routingEntries.add(new RoutingEntry(nodeID, ipLength, ipAddress, portNum));
		}

	}

	@Override
	public int getType() {
		return type;
	}

	/**
	 * @return the nR
	 */
	public int getNR() {
		return NR;
	}

	/**
	 * @return the numNodes
	 */
	public int getNumNodes() {
		return numNodes;
	}

	/**
	 * @return the routingEntries
	 */
	public List<RoutingEntry> getRoutingEntries() {
		return routingEntries;
	}

	/**
	 * @return the allNodes
	 */
	public int[] getAllNodes() {
		return allNodes;
	}

	/**
	 * Return String[] from String Delimited by '|'
	 * 
	 * @return String[]
	 */
	private String[] getRoutingEntries(String message) {
		String delimit = "\\|";
		String[] temp = message.split(delimit);
		
		if((temp.length % 4) != 0 || temp.length == 0){
			System.out.println("Routing entries were not formatted correctly. Ensure using four fields delimited by \"|\" ");
			System.exit(0);
		}
		return temp;
	}// END getParts

	/**
	 * Return int[] from String Delimited by '|'
	 * 
	 * @return int[]
	 */
	private int[] getNodeEntries(String message) {
		String delimit = "\\|";
		String[] temp = message.split(delimit);
		
		int[] nodes = new int[temp.length]; 
		for(int i = 0; i<temp.length; ++i){
			nodes[i] = Integer.parseInt(temp[i]);
		}
		return nodes;
	}// END getMessage

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RegistrySendsNodeManifest [type="
				+ type
				+ ", NR="
				+ NR
				+ ", numNodes="
				+ numNodes
				+ ", "
				+ (routingEntries != null ? "routingEntries=" + routingEntries
						+ ", " : "")
				+ (allNodes != null ? "allNodes=" + Arrays.toString(allNodes)
						: "") + "]";
	}

}
