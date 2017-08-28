package lbms.plugins.scanerss.main;

import java.net.URLDecoder;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lbms.plugins.scanerss.main.DownloadHistory.EpisodePatternMatching;

import org.jdom.Element;

/**
 * @author Damokles
 * 
 */
public class Filter implements Comparable<Filter> {

	private String					name;
	private int						id;
	private Pattern					pattern;
	private Pattern					denyPattern;
	private Pattern					clearNumberPattern;
	private DownloadHistory			dh;
	private HashDownloadHistory		hdh;
	private boolean					useDownloadHistory;
	private boolean					useHashDownloadHistory;
	private List<FilterListener>	listener		= new Vector<FilterListener>();
	private boolean					active;
	private boolean					runOnce;
	private boolean					moveAfterCompletion;
	private long					minSize			= -1;
	private long					maxSize			= -1;
	private int						minSeeds		= -1;
	private int						minLeecher		= -1;
	private String					category;
	private String					outputDir;
	private String					renameTo;
	private DownloadState			initialState	= DownloadState.DEFAULT;
	private String					azsmrcUser;
	private FileType				fileType;

	private RSSFeedManager			manager;

	public Filter(String name, String patternStr, boolean useDh, boolean useHDH) {
		this.name = name;
		setPatternString(patternStr);
		this.useDownloadHistory = useDh;
		this.id = name.hashCode() ^ patternStr.hashCode();
		this.active = true;
		if (useDh) {
			dh = new DownloadHistory(-1, -1, -1, -1);
		} else if (useHDH) {
			hdh = new HashDownloadHistory();
		}
	}

	public Filter(Element e) {
		this.name = e.getAttributeValue("name");
		this.category = e.getAttributeValue("category");
		this.outputDir = e.getAttributeValue("outputDir");
		this.renameTo = e.getAttributeValue("renameTo");
		this.azsmrcUser = e.getAttributeValue("azsmrcUser");
		try {
			this.id = e.getAttribute("id").getIntValue();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		this.active = Boolean.parseBoolean(e.getAttributeValue("active"));
		this.runOnce = Boolean.parseBoolean(e.getAttributeValue("runOnce"));
		this.moveAfterCompletion = Boolean.parseBoolean(e
				.getAttributeValue("moveAfterCompletion"));
		setPatternString(e.getAttributeValue("pattern"));
		setDenyPattern(e.getAttributeValue("denyPattern"));
		Element dhe = e.getChild(DownloadHistory.getElementName());
		if (dhe != null) {
			this.useDownloadHistory = true;
			this.dh = new DownloadHistory(dhe);
		} else {
			this.useDownloadHistory = false;
			Element hdhe = e.getChild(HashDownloadHistory.getElementName());
			if (hdhe != null) {
				this.useHashDownloadHistory = true;
				this.hdh = new HashDownloadHistory(hdhe);
			} else {
				this.useHashDownloadHistory = false;
			}
		}
		String initState = e.getAttributeValue("initialState");
		if (initState != null) {
			this.initialState = DownloadState.valueOf(initState);
		}
		if (e.getAttribute("minSize") != null) {
			minSize = Long.parseLong(e.getAttributeValue("minSize"));
		}
		if (e.getAttribute("maxSize") != null) {
			maxSize = Long.parseLong(e.getAttributeValue("maxSize"));
		}
		if (e.getAttribute("minSeeds") != null) {
			minSeeds = Integer.parseInt(e.getAttributeValue("minSeeds"));
		}
		if (e.getAttribute("minLeecher") != null) {
			minLeecher = Integer.parseInt(e.getAttributeValue("minLeecher"));
		}
	}

	/**
	 * @param manager the manager to set
	 */
	public void setRSSFeedManager (RSSFeedManager manager) {
		this.manager = manager;
	}

	/**
	 * Checks if an Torrent matches the Download criteria and adds it to the
	 * DownloadHistory if used.
	 * 
	 * @param item The RSS item to check
	 * @return true if matched all criteria
	 */
	public boolean apply (RSSItem item) {
		if (!active) {
			return false;
		}
		if (apply(item.getTitle(), item.getLink())) {
			if (runOnce) {
				active = false;
			}
			item.setParentFilter(this);
			for (FilterListener l : listener) {
				l.accepted(item);
			}
			return true;
		}
		return false;
	}

	/**
	 * Checks if an Torrent matches the Download criteria and adds it to the
	 * DownloadHistory if used. Doesn't call the filter listeners.
	 * 
	 * @param title
	 * @param url
	 * @return true if it matched all criteria
	 */
	public boolean apply (String title, String url) {
		if (!active) {
			return false;
		}
		try {
			url = URLDecoder.decode(url, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (pattern.matcher(title).matches() || pattern.matcher(url).matches()) {
			if (denyPattern != null
					&& (denyPattern.matcher(title).matches() || denyPattern
							.matcher(url).matches())) {
				return false;
			}
			if (useDownloadHistory) {
				logMsg("Item: " + title + " passed Filter " + getName()
						+ " checking DownloadHistory now.");
				if (clearNumberPattern != null
						&& dh.getEpPatternMode() == EpisodePatternMatching.Normal) {
					title = clearNumberPattern.matcher(title).replaceFirst("");
					logMsg("Item: " + title
							+ " contained numbers and was cleaned up.");
				}
				Episode e = new Episode(title, manager.getActionProvider(), dh
						.getEpPatternMode(), dh.getCustomEpPattern());
				if (dh.checkAndAdd(e)) {
					logMsg("Item: "
							+ title
							+ " passed DownloadHistory and is added to the Download.");
					return true;
				} else {
					logMsg("Item: "
							+ title
							+ " failed DownloadHistory already downloaded once.");
					return false;
				}

			} else if (useHashDownloadHistory) {
				logMsg("Item: " + title + " passed Filter " + getName()
						+ " checking HashDownloadHistory now.");
				if (hdh.checkAndAdd(title)) {
					logMsg("Item: "
							+ title
							+ " passed HashDownloadHistory and is added to the Download.");
					return true;
				} else {
					logMsg("Item: "
							+ title
							+ " failed HashDownloadHistory already downloaded once.");
					return false;
				}
			} else {
				logMsg("Item: " + title + " passed Filter " + getName()
						+ " and is added to the Download.");
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks the Torrent constraints like maxSize
	 * 
	 * @param torrent torrent to test
	 * @return true if all passed, false if any of the constraints failed
	 */
	public boolean apply (RSSItem item, ITorrent torrent) {
		boolean addOK = true;
		if (getMinSize() == -1 || getMinSize() <= torrent.getSize()) {

			if (getMaxSize() > 0 && getMaxSize() < torrent.getSize()) {
				logMsg(item.getTitle() + " failed maxSize restriction "
						+ torrent.getSize() + " > " + getMaxSize());
				addOK = false;
			}
		} else {
			logMsg(item.getTitle() + " failed minSize requirement "
					+ torrent.getSize() + " < " + getMinSize());
			addOK = false;
		}

		if (addOK && (getMinSeeds() > 0 || getMinLeecher() > 0)) {
			ScrapeResult sr = Scraper.scrapeTorrent(torrent);
			if (sr != null) {
				if (sr.hasFailed()) {
					logMsg("Couldn't Scrape " + item.getTitle()
							+ ", ignoring Seed/Leecher rules. ["
							+ sr.getFailureReason() + "]");
				} else {
					if (getMinSeeds() > 0 && sr.getSeeds() < getMinSeeds()) {
						logMsg(item.getTitle()
								+ " failed minSeeds requirement "
								+ sr.getSeeds() + " < " + getMinSeeds());
						addOK = false;
					} else if (getMinLeecher() > 0
							&& sr.getLeechers() < getMinLeecher()) {
						logMsg(item.getTitle()
								+ " failed minLeecher requirement "
								+ sr.getLeechers() + " < " + getMinLeecher());
					}
				}
			} else {
				logMsg("Couldn't Scrape " + item.getTitle()
						+ ", ignoring Seed/Leecher rules.");
			}
		}
		if (!addOK) {
			removeEpisode(item);
		}
		return addOK;
	}

	public boolean checkOnly (RSSItem item) {
		return checkOnly(item.getTitle(), item.getLink());
	}

	/**
	 * Checks if an Torrent matches the Download criteria.
	 * 
	 * @param title
	 * @param url
	 * @return
	 */
	public boolean checkOnly (String title, String url) {
		try {
			url = URLDecoder.decode(url, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (pattern.matcher(title).matches() || pattern.matcher(url).matches()) {
			if (denyPattern != null
					&& (denyPattern.matcher(title).matches() || denyPattern
							.matcher(url).matches())) {
				return false;
			}
			if (useDownloadHistory) {
				Episode e = new Episode(title, manager.getActionProvider(), dh
						.getEpPatternMode(), dh.getCustomEpPattern());
				if (dh.checkAndAdd(e)) {
					return true;
				}
				return false;
			} else if (useHashDownloadHistory) {
				if (hdh.check(title)) {
					return true;
				} else {
					return false;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	/**
	 * If Normal DownloadHistory was used unset this episode
	 * 
	 * @param i
	 */
	public void removeEpisode (RSSItem i) {
		if (useDownloadHistory) {
			dh.remove(i.getEpisode());
		}
	}

	public Element toElement () {
		Element e = new Element(getElementName());
		e.setAttribute("name", getName());
		e.setAttribute("pattern", getPatternString());
		e.setAttribute("active", Boolean.toString(active));
		e.setAttribute("runOnce", Boolean.toString(runOnce));
		e.setAttribute("moveAfterCompletion", Boolean
				.toString(moveAfterCompletion));
		if (minSize > 0) {
			e.setAttribute("minSize", Long.toString(minSize));
		}
		if (maxSize > 0) {
			e.setAttribute("maxSize", Long.toString(maxSize));
		}
		if (minSeeds > 0) {
			e.setAttribute("minSeeds", Integer.toString(minSeeds));
		}
		if (minLeecher > 0) {
			e.setAttribute("minLeecher", Integer.toString(minLeecher));
		}
		if (denyPattern != null) {
			e.setAttribute("denyPattern", getDenyPatternString());
		}
		e.setAttribute("id", Integer.toString(getId()));
		if (category != null) {
			e.setAttribute("category", category);
		}
		if (outputDir != null) {
			e.setAttribute("outputDir", outputDir);
		}
		if (renameTo != null) {
			e.setAttribute("renameTo", renameTo);
		}
		if (azsmrcUser != null) {
			e.setAttribute("azsmrcUser", azsmrcUser);
		}
		if (useDownloadHistory) {
			e.addContent(dh.toElement());
		}
		if (useHashDownloadHistory) {
			e.addContent(hdh.toElement());
		}
		if (initialState != DownloadState.DEFAULT) {
			e.setAttribute("initialState", initialState.name());
		}
		return e;
	}

	public static String getElementName () {
		return "Filter";
	}

	/**
	 * @return the name
	 */
	public String getName () {
		return name;
	}

	/**
	 * @return the patternString
	 */
	public String getPatternString () {
		return pattern.toString();
	}

	public String getDenyPatternString () {
		if (denyPattern == null) {
			return "";
		}
		return denyPattern.toString();
	}

	/**
	 * @return the id
	 */
	public int getId () {
		return id;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive (boolean active) {
		this.active = active;
	}

	/**
	 * @return the active
	 */
	public boolean isActive () {
		return active;
	}

	/**
	 * @return the runOnce
	 */
	public boolean isRunOnce () {
		return runOnce;
	}

	/**
	 * @param runOnce the runOnce to set
	 */
	public void setRunOnce (boolean runOnce) {
		this.runOnce = runOnce;
	}

	public void setUseDownloadHistory (boolean useDh) {
		if (this.useDownloadHistory != useDh) {
			if (useDh) {
				dh = new DownloadHistory(-1, -1, -1, -1);
				useHashDownloadHistory = false;
				hdh = null;
			} else {
				dh = null;
			}
			this.useDownloadHistory = useDh;
		}
	}

	public boolean isUsingDownloadHistory () {
		return useDownloadHistory;
	}

	/**
	 * @return the dh
	 */
	public DownloadHistory getDownloadHistory () {
		return dh;
	}

	public void setUseHashDownloadHistory (boolean useHDH) {
		if (this.useHashDownloadHistory != useHDH) {
			if (useHDH) {
				hdh = new HashDownloadHistory();
				useDownloadHistory = false;
				dh = null;
			} else {
				hdh = null;
			}
			this.useHashDownloadHistory = useHDH;
		}
	}

	public boolean isUsingHashDownloadHistory () {
		return useHashDownloadHistory;
	}

	/**
	 * @return the dh
	 */
	public HashDownloadHistory getHashDownloadHistory () {
		return hdh;
	}

	/**
	 * @param name the name to set
	 */
	public void setName (String name) {
		this.name = name;
	}

	/**
	 * @param category the category to set
	 */
	public void setCategory (String category) {
		if (category.equals("")) {
			this.category = null;
		} else {
			this.category = category;
		}
	}

	/**
	 * @return the category
	 */
	public String getCategory () {
		return category;
	}

	/**
	 * @return the outputDir
	 */
	public String getOutputDir () {

		return outputDir;
	}

	/**
	 * @param outputDir the outputDir to set
	 */
	public void setOutputDir (String outputDir) {
		if (outputDir.equals("")) {
			this.outputDir = null;
		} else {
			this.outputDir = outputDir;
		}
	}

	/**
	 * @return the renameTo
	 */
	public String getRenameTo () {
		return renameTo;
	}

	/**
	 * @param renameTo the renameTo to set
	 */
	public void setRenameTo (String renameTo) {
		if (renameTo.equals("")) {
			this.renameTo = null;
		} else {
			this.renameTo = renameTo;
		}
	}

	/**
	 * @param patternString the patternString to set
	 */
	public void setPatternString (String patternString) {
		this.pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
		if (patternString.contains("|")) {
			patternString = patternString.substring(0, patternString
					.indexOf("|"));
		}
		Pattern number = Pattern.compile("\\d+(:?.*?\\d+)*",
				Pattern.CASE_INSENSITIVE);
		Matcher x = number.matcher(patternString);
		try {
			if (x.find()) {
				clearNumberPattern = Pattern.compile(Pattern.quote(x.group()),
						Pattern.CASE_INSENSITIVE);
			} else {
				clearNumberPattern = null;
			}
		} catch (PatternSyntaxException e) {
			clearNumberPattern = null;
			e.printStackTrace();
		}
	}

	public void setDenyPattern (String patternString) {
		if (patternString == null || patternString.equals("")) {
			this.denyPattern = null;
		} else {
			this.denyPattern = Pattern.compile(patternString,
					Pattern.CASE_INSENSITIVE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo (Filter o) {
		return name.compareToIgnoreCase(o.name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (Object obj) {
		// TODO Auto-generated method stub
		if (obj instanceof Filter) {
			Filter other = (Filter) obj;
			return name.equalsIgnoreCase(other.name);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode () {
		// TODO Auto-generated method stub
		return name.hashCode();
	}

	private void logMsg (String msg) {
		if (manager.getActionProvider() != null) {
			manager.getActionProvider().log(ActionProvider.LOG_DEBUG, msg);
		}
	}

	public void addFilterListener (FilterListener l) {
		listener.add(l);
	}

	public void removeFilterListener (FilterListener l) {
		listener.remove(l);
	}

	protected int generateNewID () {
		return ++id;
	}

	/**
	 * @return the initialState
	 */
	public DownloadState getInitialState () {
		return initialState;
	}

	/**
	 * @param initialState the initialState to set
	 */
	public void setInitialState (DownloadState initialState) {
		this.initialState = initialState;
	}

	/**
	 * @return the azsmrcUser
	 */
	public String getAzsmrcUser () {
		return azsmrcUser;
	}

	/**
	 * @param azsmrcUser the azsmrcUser to set
	 */
	public void setAzsmrcUser (String azsmrcUser) {
		if (azsmrcUser.equals("")) {
			this.azsmrcUser = null;
		} else {
			this.azsmrcUser = azsmrcUser;
		}
	}

	/**
	 * @return the minSize
	 */
	public long getMinSize () {
		return minSize;
	}

	/**
	 * @param minSize the minSize to set
	 */
	public void setMinSize (long minSize) {
		this.minSize = minSize;
	}

	/**
	 * @return the maxSize
	 */
	public long getMaxSize () {
		return maxSize;
	}

	/**
	 * @param maxSize the maxSize to set
	 */
	public void setMaxSize (long maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * @return the minSeeds
	 */
	public int getMinSeeds () {
		return minSeeds;
	}

	/**
	 * @param minSeeds the minSeeds to set
	 */
	public void setMinSeeds (int minSeeds) {
		this.minSeeds = minSeeds;
	}

	/**
	 * @return the minLeecher
	 */
	public int getMinLeecher () {
		return minLeecher;
	}

	/**
	 * @param minLeecher the minLeecher to set
	 */
	public void setMinLeecher (int minLeecher) {
		this.minLeecher = minLeecher;
	}

	/**
	 * @return the moveAfterCompletion
	 */
	public boolean isMoveAfterCompletion () {
		return moveAfterCompletion;
	}

	/**
	 * @param moveAfterCompletion the moveAfterCompletion to set
	 */
	public void setMoveAfterCompletion (boolean moveAfterCompletion) {
		this.moveAfterCompletion = moveAfterCompletion;
	}

	public static enum DownloadState {
		DEFAULT, QUEUED, STOPPED, FORCEDSTART;
	}
}
