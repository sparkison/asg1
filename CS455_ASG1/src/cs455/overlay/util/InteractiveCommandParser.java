/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import cs455.overlay.node.MessagingNode;
import cs455.overlay.node.Registry;

public class InteractiveCommandParser{

	// Instance variables **************
	Registry registeryNode;
	MessagingNode messageNode;

	// Constructor for Registry **************
	public InteractiveCommandParser(Registry registeryNode){

		this.registeryNode = registeryNode;

		try
		{
			BufferedReader fromConsole = new BufferedReader(new InputStreamReader(System.in));
			String message;

			while (true) 
			{
				//System.out.print("Enter a command: ");
				message = fromConsole.readLine();
				handleMessageFromRegistryUI(message);
			}

		} 
		catch (Exception ex) 
		{
			System.out.println("Unexpected error while reading from console!");
			ex.printStackTrace();
		}
	}

	// Constructor for MessagingNode **************
	public InteractiveCommandParser(MessagingNode messageNode){

		this.messageNode = messageNode;

		try
		{
			BufferedReader fromConsole = new BufferedReader(new InputStreamReader(System.in));
			String message;

			while (true) 
			{
				//System.out.print("Enter a command: ");
				message = fromConsole.readLine();
				handleMessageFromClientUI(message);
			}

		} 
		catch (Exception ex) 
		{
			System.out.println("Unexpected error while reading from console!");
			ex.printStackTrace();
		}
	}

	/**
	 * Used to parse commands from Registry
	 * @param message
	 */
	private void handleMessageFromRegistryUI(String message){
		if(message.startsWith("list-messaging")){
			registeryNode.printNodes();
		}else if(message.equals("list-routing-tables")){
			registeryNode.listRoutingTables();
		}else if(message.startsWith("setup-overlay")){
			int NR = registeryNode.getNRSize();
			try{
				NR = Integer.parseInt(message.split(" ")[1]);
				registeryNode.setNRSize(NR);
			}catch(ArrayIndexOutOfBoundsException e){} // Not doing anything here, in case want to use default NR of 3
			if(NR < 1){
				System.out.println("Unable to setup routing table with NR = " + NR + ", the minimum is 1.");
			}else{
				registeryNode.sendNodeManifest();
			}
		}else if(message.startsWith("start")){
			int numMessages;
			try{
				numMessages = Integer.parseInt(message.split(" ")[1]);
				if(numMessages < 1){
					System.out.println("Need a minimum of 1 packet to start, you entered " + numMessages);
				}else{
					registeryNode.requestTaskInitiate(numMessages);
				}
			}catch(ArrayIndexOutOfBoundsException e){
				System.out.println("Incorrect format for start command, please use \"start [number-of-messages]\"");
			}

		}else{
			System.out.println("Command not recognized.");
		}
	}

	/**
	 * Used to parse commands from Client
	 * @param message
	 */
	private void handleMessageFromClientUI(String message){
		if(message.startsWith("print-counters-")){
			messageNode.printCounters();
		}else if(message.equalsIgnoreCase("exit-overlay")){
			messageNode.sendDeregistration();
		}else if(message.startsWith("list-routing")){
			messageNode.listRoutingTable();
		}else{
			System.out.println("Command not recognized.");
		}
	}

}// ************** END InteractiveCommandParser class **************
