package cs455.overlay.wireformats;

/**
 * Event interface for wireformats
 * @author sparkison
 *
 */

public interface Event {
	public byte[] getBytes();
	public int getType();
}
