/**
 * 
 */
package lbms.plugins.scanerss.main;

/**
 * @author Damokles
 *
 */
public interface RSSFeedListener {
	public void updating (RSSFeed feed);
	public void updated  (RSSFeed feed, boolean newItems);
	public void error (Exception e);
	public void error (String m);
}
