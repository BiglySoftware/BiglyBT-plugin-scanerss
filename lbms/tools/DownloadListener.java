package lbms.tools;

public interface DownloadListener {

	/**
	 * @param bytesRead bytes read so far
	 * @param bytesTotal total size may be -1 if unknown
	 */
	public void progress (long bytesRead, long bytesTotal);

	/**
	 * This is called when the dl finishes.
	 */
	public void stateChanged (int oldState, int newState);


	/**
	 * Let the Download report what it is doing.
	 * 
	 * @param msg Message
	 */
	public void debugMsg (String msg);

}
