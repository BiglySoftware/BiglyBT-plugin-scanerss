/**
 * 
 */
package lbms.plugins.scanerss.main;

/**
 * @author Damokles
 *
 */
public interface RSSFeedManagerListener {
	public void feedAdded (RSSFeed f);
	public void feedRemoved (RSSFeed f);
	public void filterAdded (Filter f);
	public void filterRemoved (Filter f);
	public void updatedComplete();
	public void reset();
}
