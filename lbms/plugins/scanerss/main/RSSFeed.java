package lbms.plugins.scanerss.main;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lbms.tools.HTTPDownload;

import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author Damokles
 * 
 */
public class RSSFeed implements Runnable, Comparable<RSSFeed> {

	public static final int			TYPE_RSS_20		= 1;
	public static final int			TYPE_RSS_10		= 2;
	public static final int			TYPE_RSS_09x	= 3;
	public static final int			TYPE_ATOM		= 4;

	private String					userAgent;
	private String					referer;
	private String					cookie;
	private String					username;
	private String					password;
	private Pattern					modLinkRegex;
	private String					modLinkReplace;
	private String					torrentLinkIdentifier;

	private String					name;
	private URL						sourceURL;
	private int						id;
	private int						updateInterval;
	private int						rssType;
	private long					lastUpdate;
	private boolean					newItems;
	private boolean					active;
	private boolean					isFile;

	private int						failed;

	private RSSFeedManager			manager;

	private List<RSSItem>			items			= new Vector<RSSItem>();
	private List<RSSItem>			newItemsList	= new Vector<RSSItem>();
	private List<RSSFeedListener>	listeners		= new Vector<RSSFeedListener>();

	public RSSFeed(String name, URL url) {
		setSourceURL(url);
		this.name = name;
		this.id = name.hashCode() ^ url.toExternalForm().hashCode();
		this.updateInterval = 108000;
		this.active = true;
	}

	public RSSFeed(Element e) {
		try {
			setSourceURL(new URL(e.getAttributeValue("url")));
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		this.name = e.getAttributeValue("name");
		this.active = Boolean.parseBoolean(e.getAttributeValue("active"));
		userAgent = e.getAttributeValue("userAgent");
		referer = e.getAttributeValue("referer");
		cookie = e.getAttributeValue("cookie");
		username = e.getAttributeValue("username");
		password = e.getAttributeValue("password");
		if (e.getAttributeValue("modLinkRegex") != null) {
			try {
				this.modLinkRegex = Pattern.compile(e
						.getAttributeValue("modLinkRegex"));
			} catch (PatternSyntaxException e1) {
				e1.printStackTrace();
			}
		}

		modLinkReplace = e.getAttributeValue("modLinkReplace");

		torrentLinkIdentifier = e.getAttributeValue("torrentLinkIdentifier");

		try {
			this.id = e.getAttribute("id").getIntValue();
			this.lastUpdate = e.getAttribute("lastUpdate").getLongValue();
			this.updateInterval = e.getAttribute("updateInterval")
					.getIntValue();
		} catch (DataConversionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * @return the items
	 */
	public List<RSSItem> getItems () {
		return items;
	}

	public int getItemCount () {
		return items.size();
	}

	/**
	 * @return the new items
	 */
	public List<RSSItem> getNewItems () {
		return newItemsList;
	}

	/**
	 * @return the lastUpdate
	 */
	public long getLastUpdate () {
		return lastUpdate;
	}

	/**
	 * @return the rssType
	 */
	public int getRssType () {
		return rssType;
	}

	/**
	 * @return the sourceURL
	 */
	public URL getSourceURL () {
		return sourceURL;
	}

	/**
	 * @return the isFile
	 */
	public boolean isFile () {
		return isFile;
	}

	/**
	 * @return the name
	 */
	public String getName () {
		return name;
	}

	/**
	 * @return the id
	 */
	public int getId () {
		return id;
	}

	/**
	 * @return the modLinkRegex
	 */
	public Pattern getModLinkRegex () {
		return modLinkRegex;
	}

	/**
	 * @param modLinkRegex the modLinkRegex to set
	 */
	public void setModLinkRegex (String modLinkRegex) {
		if (modLinkRegex == null) {
			this.modLinkRegex = null;
		} else {
			this.modLinkRegex = Pattern.compile(modLinkRegex);
		}
	}

	/**
	 * @return the modLinkReplace
	 */
	public String getModLinkReplace () {
		return modLinkReplace;
	}

	/**
	 * @param modLinkReplace the modLinkReplace to set
	 */
	public void setModLinkReplace (String modLinkReplace) {
		this.modLinkReplace = modLinkReplace;
	}

	public String getTorrentLinkIdentifier () {
		return torrentLinkIdentifier;
	}

	public void setTorrentLinkIdentifier (String torrentLinkIdentifier) {
		this.torrentLinkIdentifier = torrentLinkIdentifier;
	}

	/**
	 * @param name the name to set
	 */
	public void setName (String name) {
		this.name = name;
	}

	/**
	 * @param sourceURL the sourceURL to set
	 */
	public void setSourceURL (URL sourceURL) {
		this.sourceURL = sourceURL;
		isFile = sourceURL.getProtocol().equalsIgnoreCase("file");

	}

	/**
	 * @return the updateInterval
	 */
	public int getUpdateInterval () {
		return updateInterval;
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
	 * @return the cookie
	 */
	public String getCookie () {
		return cookie;
	}

	/**
	 * @param cookie the cookie to set
	 */
	public void setCookie (String cookie) {
		this.cookie = cookie;
	}

	/**
	 * @return the password
	 */
	public String getPassword () {
		return password;
	}

	/**
	 * @return the manager
	 */
	public RSSFeedManager getManager () {
		return manager;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword (String password) {
		this.password = password;
	}

	/**
	 * @return the referer
	 */
	public String getReferer () {
		return referer;
	}

	/**
	 * @param referer the referer to set
	 */
	public void setReferer (String referer) {
		this.referer = referer;
	}

	/**
	 * @return the userAgent
	 */
	public String getUserAgent () {
		return userAgent;
	}

	/**
	 * @param userAgent the userAgent to set
	 */
	public void setUserAgent (String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * @return the username
	 */
	public String getUsername () {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername (String username) {
		this.username = username;
	}

	/**
	 * @param manager the manager to set
	 */
	public void setRSSFeedManager (RSSFeedManager manager) {
		this.manager = manager;
	}

	/**
	 * @param updateInterval the updateInterval to set
	 */
	public void setUpdateInterval (int updateInterval) {
		this.updateInterval = updateInterval;
	}

	public Element toElement () {
		Element e = new Element(getElementName());
		e.setAttribute("name", name);
		e.setAttribute("url", sourceURL.toExternalForm());
		e.setAttribute("id", Integer.toString(id));
		e.setAttribute("lastUpdate", Long.toString(lastUpdate));
		e.setAttribute("updateInterval", Integer.toString(updateInterval));
		e.setAttribute("active", Boolean.toString(active));
		if (userAgent != null) {
			e.setAttribute("userAgent", userAgent);
		}
		if (referer != null) {
			e.setAttribute("referer", referer);
		}
		if (cookie != null) {
			e.setAttribute("cookie", cookie);
		}
		if (username != null) {
			e.setAttribute("username", username);
		}
		if (password != null) {
			e.setAttribute("password", password);
		}
		if (modLinkRegex != null) {
			e.setAttribute("modLinkRegex", modLinkRegex.pattern());
		}
		if (modLinkReplace != null) {
			e.setAttribute("modLinkReplace", modLinkReplace);
		}
		if (torrentLinkIdentifier != null) {
			e.setAttribute("torrentLinkIdentifier", torrentLinkIdentifier);
		}

		return e;
	}

	public static String getElementName () {
		return "RSSFeed";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo (RSSFeed o) {
		return name.compareTo(o.name);
	}

	public void read () throws IOException {
		if (!active) {
			return;
		}
		InputStream is = null;
		if (isFile) {
			try {
				File f = new File(sourceURL.toExternalForm().substring(7));
				if (f.exists()) {
					is = new FileInputStream(f);
					SAXBuilder builder = new SAXBuilder();
					Document xmlDom = builder.build(is);
					new XMLOutputter(Format.getPrettyFormat()).output(xmlDom,
							System.out);
					parseFeed(xmlDom);
					lastUpdate = System.currentTimeMillis() / 1000;
				} else {
					logMsg("Couldn't read Feed from File: "
							+ f.getAbsolutePath());
				}
			} catch (JDOMException e) {
				e.printStackTrace();
			} finally {
				if (is != null) {
					is.close();
				}
			}

		} else {
			try {
				HTTPDownload dl = new HTTPDownload(sourceURL);
				if (manager != null && manager.getProxy() != null) {
					dl.setProxy(manager.getProxy());
				}
				if (userAgent != null) {
					dl.setUserAgent(userAgent);
				} else if (manager != null) {
					dl.setUserAgent(manager.getActionProvider()
							.getDefaultUserAgent());
				}
				if (username != null && password != null) {
					dl.setLogin(username, password);
				}
				if (referer != null) {
					dl.setReferer(referer);
				}
				if (cookie != null) {
					dl.setCookie(cookie);
				}
				dl.call();
				if (!dl.hasFinished() || dl.hasFailed()) {
					throw new Exception("Error while downloading feed " + name + " Reason: " + dl.getFailureReason());
				}
				is = new ByteArrayInputStream(dl.getBuffer().toByteArray());
				try {
					SAXBuilder builder = new SAXBuilder();
					Document xmlDom = builder.build(is);
					new XMLOutputter(Format.getPrettyFormat()).output(xmlDom,
							System.out);
					parseFeed(xmlDom);
					lastUpdate = System.currentTimeMillis() / 1000;
				} catch (JDOMException e) {
					logMsg("Error parsing feed " + name + ": " + e.getMessage());
					e.printStackTrace();
				}

			} catch (Exception e) {
				logMsg(e.getMessage());
			}
		}
	}

	public void readThreaded () {
		if (!active) {
			return;
		}
		Thread t = new Thread(this);
		t.setPriority(Thread.MIN_PRIORITY);
		t.setDaemon(true);
		t.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run () {
		try {
			read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void failed () {
		failed++;
	}

	public int getFailed () {
		return failed;
	}

	public boolean hasNewItems () {
		return newItems;
	}

	private void parseFeed (Document data) {
		Element root = data.getRootElement();
		String rssVersion = root.getAttributeValue("version");
		List<RSSItem> oldItems = new Vector<RSSItem>(items);
		logMsg("Parsing Feed: " + getName() + ", RSS Version: " + rssVersion);
		items.clear();
		if (root.getName().equalsIgnoreCase("rss") && rssVersion != null) {
			if (rssVersion.contains("2.0")) {
				parse_20(root);
			} else if (rssVersion.contains("1.0")) {
				parse_10(root);
			} else if (rssVersion.contains("0.9")) {
				parse_09x(root);
			}
		}

		if (root.getName().equalsIgnoreCase("feed")
				&& root.getAttributeValue("xmlns").equalsIgnoreCase(
						"http://www.w3.org/2005/Atom")) {
			parse_Atom(root);
		}
		newItemsList = new Vector<RSSItem>(items);
		newItemsList.removeAll(oldItems);
		if (newItemsList.size() > 0) {
			newItems = true;
		} else {
			newItems = false;
		}
		for (RSSItem i : oldItems) {
			i.setNew(false);
		}
		for (RSSItem i : newItemsList) {
			i.setNew(true);
		}
		logMsg("Feed: " + getName() + " has " + newItemsList.size()
				+ " new Items.");
		callUpdated(newItems);
	}

	@SuppressWarnings("unchecked")
	private void parse_09x (Element root) {
		rssType = TYPE_RSS_09x;
		List<Element> rssItems = root.getChild("channel").getChildren("item");
		for (Element rssItem : rssItems) {
			String title = rssItem.getChildText("title");
			String link = rssItem.getChildText("link");
			String description = rssItem.getChildText("description");
			if (title != null && link != null) {
				try {
					new URL(link);
					RSSItem item = new RSSItem(title, link, description, this);
					items.add(item);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void parse_10 (Element root) {
		rssType = TYPE_RSS_10;

	}

	@SuppressWarnings("unchecked")
	private void parse_20 (Element root) {
		rssType = TYPE_RSS_20;
		List<Element> rssItems = root.getChild("channel").getChildren("item");
		for (Element rssItem : rssItems) {
			String title = rssItem.getChildText("title");
			String link = rssItem.getChildText("link");
			Element enclosure = rssItem.getChild("enclosure");
			link = (enclosure != null) ? enclosure.getAttributeValue("url")
					: link;
			String description = rssItem.getChildText("description");
			if (title != null && link != null) {
				try {
					new URL(link);
					RSSItem item = new RSSItem(title, link, description, this);
					items.add(item);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}

	}

	@SuppressWarnings("unchecked")
	private void parse_Atom (Element root) {
		rssType = TYPE_ATOM;
		List<Element> rssItems = root.getChildren("entry");
		for (Element rssItem : rssItems) {
			try {
				String title = rssItem.getChildText("title");
				String link = rssItem.getChild("link")
						.getAttributeValue("href");
				if (title != null && link != null) {
					try {
						new URL(link);
						RSSItem item = new RSSItem(title, link, null, this);
						items.add(item);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
	}

	private void logMsg (String msg) {
		if (manager.getActionProvider() != null) {
			manager.getActionProvider().log(ActionProvider.LOG_DEBUG, msg);
		}
	}

	public void addListener (RSSFeedListener l) {
		listeners.add(l);
	}

	public void removeListener (RSSFeedListener l) {
		listeners.remove(l);
	}

	private void callUpdated (boolean newItems) {
		for (RSSFeedListener l : listeners) {
			l.updated(this, newItems);
		}
	}

	protected int generateNewID () {
		return ++id;
	}
}
