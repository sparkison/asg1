/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.wireformats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class EventFactory {
	// Singleton instance
	private static EventFactory instance = null;

	// Exists only to defeat instantiation
	protected EventFactory() {

	}

	// Get instance of EventFactory
	public static EventFactory getInstance() {
		if (instance == null) {
			instance = new EventFactory();
		}
		return instance;
	}

	/**
	 * Precondition(s):
	 * 1.	Type is the message type based on the Protocol interface
	 * 2.	Message is String, sent with correct number of arguments
	 * 3.	Arguments are separated by ';'
	 * 
	 * Postcondition(s):
	 * 1.	Returns appropriate Node type based on 'type' argument
	 * 2.	Returned Node has getType() and getBytes() methods defined
	 * 
	 * @return Event
	 */
	public Event buildEvent(int type, String message) {

		// Get the message components
		String[] eventMessage = getMessage(message);

		switch (type) {

		case Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS:
			if (eventMessage.length == 3) {
				return new NodeReportsOverlaySetupStatus(type, 
						Integer.parseInt(eventMessage[0]), Integer.parseInt(eventMessage[1]), 
						eventMessage[2]);
			}else{
				System.out.println("Incorrect message length sent");
			}
			break;

		case Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED:
			if (eventMessage.length == 3) {
				return new OverlayNodeReportsTaskFinished(type, eventMessage[0], 
						Integer.parseInt(eventMessage[1]), Integer.parseInt(eventMessage[2]));
			}else{
				System.out.println("Incorrect message length sent");
			}
			break;

		case Protocol.OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY:
			if (eventMessage.length == 6) {
				return new OverlayNodeReportsTrafficSummary(type, Integer.parseInt(eventMessage[0]), 
						Integer.parseInt(eventMessage[1]), Integer.parseInt(eventMessage[2]), 
						Long.parseLong(eventMessage[3]), Integer.parseInt(eventMessage[4]), 
						Long.parseLong(eventMessage[5]));
			}else{
				System.out.println("Incorrect message length sent");
			}
			break;

		case Protocol.OVERLAY_NODE_SENDS_DATA:
			/**
			 * 
			 * NOTE!!
			 * Using String to hold hops, supposed to be int[]
			 * Not sure how to byte pack an int[] so using String for now...
			 * 
			 */
			if (eventMessage.length == 5) {
				return new OverlayNodeSendsData(type, Integer.parseInt(eventMessage[0]), 
						Integer.parseInt(eventMessage[1]), Integer.parseInt(eventMessage[2]), 
						Integer.parseInt(eventMessage[3]), eventMessage[4]);
			}else{
				System.out.println("Incorrect message length sent");
			}
			break;

		case Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION:
			if (eventMessage.length == 4) {
				return new OverlayNodeSendsDeregistration(type, 
						Integer.parseInt(eventMessage[0]), eventMessage[1], 
						Integer.parseInt(eventMessage[2]), Integer.parseInt(eventMessage[3]));
			}else{
				System.out.println("Incorrect message length sent");
			}
			break;

		case Protocol.OVERLAY_NODE_SENDS_REGISTRATION:
			if (eventMessage.length == 3) {
				return new OverlayNodeSendsRegistration(type,
						Integer.parseInt(eventMessage[0]), eventMessage[1],
						Integer.parseInt(eventMessage[2]));
			}else{
				System.out.println("Incorrect message length sent");
			}
			break;

		case Protocol.REGISTRY_REPORTS_DEREGISTRATION_STATUS:
			/**
			 * 
			 * Assuming message to be either 1 for success, or -1 for failure
			 * Not specified in the assignment specs.
			 * 
			 */
			if (eventMessage.length == 1) {
				return new RegistryReportsDeregistrationStatus(type, Integer.parseInt(eventMessage[0]));
			}else{
				System.out.println("Incorrect message length sent");
			}
			break;

		case Protocol.REGISTRY_REPORTS_REGISTRATION_STATUS:
			if (eventMessage.length == 3) {
				return new RegistryReportsRegistrationStatus(type, 
						Integer.parseInt(eventMessage[0]), Integer.parseInt(eventMessage[1]), 
						eventMessage[2]);
			}else{
				System.out.println("Incorrect message length sent");
			}
			break;

		case Protocol.REGISTRY_REQUESTS_TASK_INITIATE:
			if (eventMessage.length == 1) {
				return new RegistryRequestsTaskInitiate(type, Integer.parseInt(eventMessage[0]));
			}else{
				System.out.println("Incorrect message length sent");
			}
			break;

		case Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY:
			return new RegistryRequestsTrafficSummary(type);

		case Protocol.REGISTRY_SENDS_NODE_MANIFEST:
			if (eventMessage.length == 4) {
				return new RegistrySendsNodeManifest(type, Integer.parseInt(eventMessage[0]), 
						eventMessage[1], eventMessage[2], Integer.parseInt(eventMessage[3]));
			}else{
				System.out.println("Incorrect message length sent");
			}
			break;

		default:
			//System.out.println("Unrecognized event type: " + type + ", unable to build Event.");

		}

		return null;
	}// END buildEvent


	/**
	 * Precondition(s):
	 * 1.	Type is the message type based on the Protocol interface
	 * 2.	Passed byte[] data for constructing the event
	 * 
	 * Postcondition(s):
	 * 1.	Returns appropriate Node type based on 'type' argument
	 * 2.	Returned Node has getType() and getBytes() methods defined
	 * 
	 * @return Event
	 */
	public Event getEvent(byte[] components) {

		int type = getType(components);

		switch (type) {

		case Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS:
			try {
				return new NodeReportsOverlaySetupStatus(components);
			} catch (IOException e) {
				System.out.println("Error creating NodeReportsOverlaySetupStatus");
				e.printStackTrace();
			}

		case Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED:
			try {
				return new OverlayNodeReportsTaskFinished(components);
			} catch (IOException e) {
				System.out.println("Error creating OverlayNodeReportsTaskFinished");
				e.printStackTrace();
			}

		case Protocol.OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY:
			try {
				return new OverlayNodeReportsTrafficSummary(components);
			} catch (IOException e) {
				System.out.println("Error creating OverlayNodeReportsTrafficSummary");
				e.printStackTrace();
			}

		case Protocol.OVERLAY_NODE_SENDS_DATA:
			try {
				return new OverlayNodeSendsData(components);
			} catch (IOException e) {
				System.out.println("Error creating OverlayNodeSendsData");
				e.printStackTrace();
			}

		case Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION:
			try {
				return new OverlayNodeSendsDeregistration(components);
			} catch (IOException e) {
				System.out.println("Error creating OverlayNodeSendsDeregistration");
				e.printStackTrace();
			}

		case Protocol.OVERLAY_NODE_SENDS_REGISTRATION:
			try {
				return new OverlayNodeSendsRegistration(components);
			} catch (IOException e) {
				System.out.println("Error creating OverlayNodeSendsRegistration");
				e.printStackTrace();
			}

		case Protocol.REGISTRY_REPORTS_DEREGISTRATION_STATUS:
			try {
				return new RegistryReportsDeregistrationStatus(components);
			} catch (IOException e) {
				System.out.println("Error creating RegistryReportsDeregistrationStatus");
				e.printStackTrace();
			}

		case Protocol.REGISTRY_REPORTS_REGISTRATION_STATUS:
			try {
				return new RegistryReportsRegistrationStatus(components);
			} catch (IOException e) {
				System.out.println("Error creating RegistryReportsRegistrationStatus");
				e.printStackTrace();
			}

		case Protocol.REGISTRY_REQUESTS_TASK_INITIATE:
			try {
				return new RegistryRequestsTaskInitiate(components);
			} catch (IOException e) {
				System.out.println("Error creating RegistryRequestsTaskInitiate");
				e.printStackTrace();
			}

		case Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY:
			try {
				return new RegistryRequestsTrafficSummary(components);
			} catch (IOException e) {
				System.out.println("Error creating RegistryRequestsTrafficSummary");
				e.printStackTrace();
			}

		case Protocol.REGISTRY_SENDS_NODE_MANIFEST:
			try {
				return new RegistrySendsNodeManifest(components);
			} catch (IOException e) {
				System.out.println("Error creating RegistrySendsNodeManifest");
				e.printStackTrace();
			}

		default:
			return null;
			//System.out.println("Unrecognized event type: " + type + ", unable to get Event.");

		}

	}


	/********************************************
	 ****************** HELPERS *****************
	 ********************************************/


	/**
	 * Get the type based on byte[] passed
	 * @param data
	 * @return int
	 */
	private int getType(byte[] data){
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
		DataInputStream din = new DataInputStream(baInputStream);
		int type = -1;
		try {
			type = din.readInt();
			din.close();
			baInputStream.close();
		} catch (IOException e) {
			System.out.println("EventFactory - error getting data type: ");
			e.printStackTrace();
		}
		return type;
	}// END getType **************

	/**
	 * Gets the string from the getEvent method, parameters delimited by ';'
	 * 
	 * @return String
	 */
	private String[] getMessage(String message) {
		String delimit = ";";
		String[] temp = message.split(delimit);
		return temp;
	}// END getMessage

}// ************** END EventFactory class **************