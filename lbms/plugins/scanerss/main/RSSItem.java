package lbms.plugins.scanerss.main;



/**
 * @author Damokles
 *
 */
public class RSSItem implements Comparable<RSSItem> {
	private String title;
	private String link;
	private String description;
	private boolean newItem;
	private int hashCode;
	private RSSFeed parentFeed;
	private Filter parentFilter;
	private long timeStamp;
	private Episode episode;

	public RSSItem(String title, String link, String description, RSSFeed parent) {
		super();
		this.parentFeed = parent;
		this.title = title;
		this.link = link;
		this.description = (description != null) ? description:"";
		this.hashCode = (title+link).hashCode();
		this.timeStamp = System.currentTimeMillis();
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @return the link
	 */
	public String getLink() {
		return link;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the episode
	 */
	public Episode getEpisode() {
		if (episode == null) {
			ActionProvider ap = null;
			if (parentFeed != null && parentFeed.getManager() != null)
				ap = parentFeed.getManager().getActionProvider();
			episode = new Episode(title, ap);
		}
		return episode;
	}

	public void setNew (boolean isNew) {
		this.newItem = isNew;
	}

	public boolean isNew() {
		return newItem;
	}

	/**
	 * @return the timeStamp
	 */
	public long getTimeStamp() {
		return timeStamp;
	}


	/**
	 * @return the parentFilter
	 */
	public Filter getParentFilter() {
		return parentFilter;
	}
	/**
	 * @param parentFilter the parentFilter to set
	 */
	public void setParentFilter(Filter parentFilter) {
		this.parentFilter = parentFilter;
	}
	/**
	 * @return the parentFeed
	 */
	public RSSFeed getParentFeed() {
		return parentFeed;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "<RSSItem>\nTitle: "+title+"\nLink: "+link+"\nDescription: "+description+"\n</RSSItem>";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RSSItem) {
			RSSItem rssItem = (RSSItem) obj;
			if (title.equals(rssItem.title) && link.equals(rssItem.link)) return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(RSSItem o) {
		return title.compareTo(o.title);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hashCode;
	}
}
