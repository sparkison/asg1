/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cs455.overlay.transport.TCPConnectionThread;

public class RoutingTable {

	// Instance variables **************
	private int nrSize;
	private List<RoutingEntry> entries;

	// Constructor **************
	public RoutingTable(int nrSize, List<RoutingEntry> entries){
		this.nrSize = nrSize;
		this.entries = entries;

		// Sort the list
		Collections.sort(entries);
	}

	/**
	 * Used to get and format the hop list return String
	 * This is the format needed for the 
	 * RegistrySendsNodeManifest class 
	 * @param client
	 * @param nodeID
	 * @return String
	 */
	public String getNodesTable(int nodeID){
		
		String hopList = "";

		int nodeLocation = -1;

		for(int j = 0; j<entries.size(); ++j){
			if(entries.get(j).getNodeID() == nodeID){
				nodeLocation = j;
				break;
			}
		}

		List<RoutingEntry> myHopList = new ArrayList<RoutingEntry>();

		if(nodeLocation == -1){
			// We didn't find the node in the list
			System.out.println("The node id ("+ nodeID +") doesn't match any node in the routing table. Please ensure correct id entered.");
		}else if(entries.size() < 2){
			System.out.println("You only have one node in the routing entries table. You must add a minimum of two nodes.");
		}else{
			// We've found the node at nodeLocation
			// Loop NR times to get routing table
			for(int i = 0; i<nrSize; ++i){
				// Get our "hop", making sure not to go out of bounds
				int hop = ((int)Math.pow(2,i) + nodeLocation) % entries.size();
				if(entries.get(hop).getNodeID() == nodeID){
					// Need to advance one more, can't add self
					entries.get((hop + 1) % entries.size());
				}else{
					myHopList.add(entries.get(hop));
				}
			}
		}

		// Get the list in a formatted String
		for(int i = 0; i<myHopList.size(); ++i){
			hopList += myHopList.get(i).getNodeID() + "|";
			hopList += myHopList.get(i).getIpLength() + "|";
			hopList += myHopList.get(i).getIpAddress() + "|";
			// If last entry, make sure we don't add an extra delimiter
			if(i != myHopList.size()-1){
				hopList += myHopList.get(i).getPortNum() + "|";
			}else{
				hopList += myHopList.get(i).getPortNum();
			}
		}

		return hopList;

	}

	/**
	 * Used to format the nodes return String
	 * This is the format needed for the 
	 * RegistrySendsNodeManifest class
	 * @return String
	 */
	public String getNodesList(){

		String nodes = "";

		for(int j = 0; j<entries.size(); ++j){
			// If last entry, make sure we don't add an extra delimiter
			if(j != entries.size()-1){
				nodes += entries.get(j).getNodeID() + "|";
			}else{
				nodes += entries.get(j).getNodeID();
			}
		}

		return nodes;
	}

	/**
	 * Used by the Registry command parser for debugging
	 * @return
	 */
	public String getRoutingTables(){

		String hopList = "";

		int nodeLocation;
		int nodeID;
		List<RoutingEntry> myHopList = new ArrayList<RoutingEntry>();
		String leftAlignFormat = "| %-11s | %-17s | %-5d |%n";
		
		// O(n^2) operation!! Should look into optimizing....
		
		for(int i = 0; i<entries.size(); ++i){

			nodeLocation = i;
			nodeID = entries.get(i).getNodeID();

			// Reset the list for each iteration
			myHopList.clear();
			
			hopList += String.format("+-----------------------------------------+%n");
			hopList += String.format("| NODE %4d's ROUTING TABLE               |%n", nodeID);			
			hopList += String.format("+-------------+-------------------+-------+%n");
			hopList += String.format("| NODE ID     | IP ADDRESS        | PORT  |%n");
			hopList += String.format("+-------------+-------------------+-------+%n");

			// Loop NR times to get routing table
			for(int k = 0; k<nrSize; ++k){
				// Get our "hop", making sure not to go out of bounds
				int hop = ((int)Math.pow(2,k) + nodeLocation) % entries.size();
				if(entries.get(hop).getNodeID() == nodeID){
					// Need to advance one more, can't add self
					entries.get((hop + 1) % entries.size());
				}else{
					myHopList.add(entries.get(hop));
				}
			}

			// Get the list in a formatted String
			for(int l = 0; l<myHopList.size(); ++l){
				hopList += String.format(leftAlignFormat,  myHopList.get(l).getNodeID(), myHopList.get(l).getIpAddress(), myHopList.get(l).getPortNum());
			}
			hopList += String.format("+-------------+-------------------+-------+%n");
			hopList += "\n\n\n";

		}
		
		return hopList;

	}
	
	/**
	 * Used to clear the table
	 */
	public void clear(){
		nrSize = 0;
		entries.clear();
	}
	
	public boolean isEmpty(){
		return entries.isEmpty();
	}

}// ************** END RoutingTable class **************