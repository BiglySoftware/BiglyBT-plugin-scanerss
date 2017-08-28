package lbms.plugins.scanerss.main;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author Damokles
 * 
 */
public class QueuedRSSItem implements Delayed {

	private long	timestamp;
	private RSSItem	rssItem;

	/**
	 * @param rssItem RSSItem to delay
	 * @param delay in Microseconds
	 */
	public QueuedRSSItem (RSSItem rssItem, long delay) {
		this(rssItem, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * @param rssItem RSSItem to delay
	 * @param delay delay in the given TimeUnit
	 * @param unit TimeUnit of the delay
	 */
	public QueuedRSSItem (RSSItem rssItem, long delay, TimeUnit unit) {
		this.rssItem = rssItem;
		timestamp = System.currentTimeMillis()
				+ TimeUnit.MILLISECONDS.convert(delay, unit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	public long getDelay (TimeUnit unit) {
		return unit.convert(timestamp - System.currentTimeMillis(),
				TimeUnit.MILLISECONDS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo (Delayed o) {
		long x = getDelay(TimeUnit.MILLISECONDS)
				- o.getDelay(TimeUnit.MILLISECONDS);
		if (x > 0) {
			return 1;
		} else if (x < 0) {
			return -1;
		}
		return 0;
	}

	/**
	 * @return the rssItem
	 */
	public RSSItem getRssItem () {
		return rssItem;
	}
}
