package cs455.overlay.util;

import java.util.List;

import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.OverlayNodeReportsTrafficSummary;

public class StatisticsCollectorAndDisplay {

	// Instance variables **************
	List<Event> nodesSummary;
	
	public void sendNodeData(List<Event> nodesSummary){
		this.nodesSummary = nodesSummary;
		buildTable();
	}
	
	private void buildTable(){
		
		// Formatting
		String statsTable = "\n";
		long totalPacketsSent = 0, totalPacketsReceived = 0, totalPacketsRelayed = 0, totalSendSum = 0, totalReceiveSum = 0;
		
		String leftAlignFormat = "| %-8s | %-13s | %-17d | %-16s | %-18s | %-18s |%n";
		
		statsTable += String.format("           +---------------+-------------------+------------------+--------------------+--------------------+%n");
		statsTable += String.format("           | PACKETS SENT  | PACKETS RECEIVED  | PACKETS RELAYED  | SEND SUMMATION     | RECEIVE SUMMATION  |%n");
		statsTable += String.format("+----------+---------------+-------------------+------------------+--------------------+--------------------+%n");
		
		for(Event event : nodesSummary){
						
			OverlayNodeReportsTrafficSummary nodeSummary = (OverlayNodeReportsTrafficSummary) event;
			
			totalPacketsSent += nodeSummary.getNumPacketsSent();
			totalPacketsReceived += nodeSummary.getNumPacketsReceived();
			totalPacketsRelayed += nodeSummary.getNumPacketsRelayed();
			totalSendSum += nodeSummary.getSumPacketDataSent();
			totalReceiveSum += nodeSummary.getSumPacketDataReceived();
			
			statsTable += String.format(leftAlignFormat, "NODE " + nodeSummary.getNodeID(), 
					nodeSummary.getNumPacketsSent(), nodeSummary.getNumPacketsReceived(), 
					nodeSummary.getNumPacketsRelayed(), nodeSummary.getSumPacketDataSent(), 
					nodeSummary.getSumPacketDataReceived()
					);
			statsTable += String.format("+----------+---------------+-------------------+------------------+--------------------+--------------------+%n");
			
		}
		statsTable += String.format(leftAlignFormat, "SUM", totalPacketsSent, totalPacketsReceived, totalPacketsRelayed, totalSendSum, totalReceiveSum);
		statsTable += String.format("+----------+---------------+-------------------+------------------+--------------------+--------------------+%n");
		
		// Print the table!
		System.out.println(statsTable);
		
	}
	
}
