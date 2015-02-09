package cs455.overlay.node;

import cs455.overlay.wireformats.Event;

public interface Node {
	
	// Event with id of connection that sent it
	public void onEvent(Event e, int id);
	// Close method, called if socket exception
	public void close();
	
}
