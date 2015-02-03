/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.routing;

public class RoutingEntry implements Comparable<RoutingEntry>{

		private int nodeID;
		private int ipLength;
		private String ipAddress;
		private int portNum;
		
		public RoutingEntry(int nodeID, int ipLength, String ipAddress, int portNum){
			this.nodeID = nodeID;
			this.ipLength = ipLength;
			this.ipAddress = ipAddress;
			this.portNum = portNum;
		}

		/**
		 * @return the nodeID
		 */
		public int getNodeID() {
			return nodeID;
		}

		/**
		 * @return the ipLength
		 */
		public int getIpLength() {
			return ipLength;
		}

		/**
		 * @return the ipAddress
		 */
		public String getIpAddress() {
			return ipAddress;
		}

		/**
		 * @return the portNum
		 */
		public int getPortNum() {
			return portNum;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "RoutingEntry [nodeID="
					+ nodeID
					+ ", ipLength="
					+ ipLength
					+ ", "
					+ (ipAddress != null ? "ipAddress=" + ipAddress + ", " : "")
					+ "portNum=" + portNum + "]";
		}

		@Override
		public int compareTo(RoutingEntry r) {

			if(this.nodeID == r.nodeID) return 0;
			if(this.nodeID < r.nodeID) return -1;
			if(this.nodeID > r.nodeID) return 1;

			return 0;
		}

		
}// ************** END RoutingEntry class **************
