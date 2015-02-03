/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.overlay.node;

import cs455.overlay.transport.TCPConnectionThread;
import cs455.overlay.wireformats.Event;

public interface Node {
	public void onEvent(Event e, TCPConnectionThread client);
}
