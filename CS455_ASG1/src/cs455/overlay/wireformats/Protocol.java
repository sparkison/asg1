package cs455.overlay.wireformats;

public interface Protocol {
	final static int OVERLAY_NODE_SENDS_REGISTRATION 		= 2;
	final static int REGISTRY_REPORTS_REGISTRATION_STATUS 	= 3;
	final static int OVERLAY_NODE_SENDS_DEREGISTRATION 		= 4;
	final static int REGISTRY_REPORTS_DEREGISTRATION_STATUS = 5;
	final static int REGISTRY_SENDS_NODE_MANIFEST 			= 6;
	final static int NODE_REPORTS_OVERLAY_SETUP_STATUS 		= 7;
	final static int REGISTRY_REQUESTS_TASK_INITIATE 		= 8;
	final static int OVERLAY_NODE_SENDS_DATA 				= 9;
	final static int OVERLAY_NODE_REPORTS_TASK_FINISHED 	= 10;
	final static int REGISTRY_REQUESTS_TRAFFIC_SUMMARY 		= 11;
	final static int OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY 	= 12;
}