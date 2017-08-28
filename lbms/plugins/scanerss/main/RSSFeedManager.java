package lbms.plugins.scanerss.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
public class RSSFeedManager {

	private List<RSSFeed>					feeds				= new Vector<RSSFeed>();
	private List<FileType>					fileTypes			= new Vector<FileType>();
	private Map<Integer, Filter>			filter				= new HashMap<Integer, Filter>();
	private Map<Integer, Set<Filter>>		links				= new HashMap<Integer, Set<Filter>>();

	private List<RSSFeedManagerListener>	listeners			= new Vector<RSSFeedManagerListener>();

	private RSSFeed[]						emptyFeedArray		= new RSSFeed[0];
	private Filter[]						emptyFilterArray	= new Filter[0];

	private boolean							updating			= false;
	private Proxy							proxy;

	private RSSFeedListener					feedListener		= new RSSFeedListener() {
			public void updated (RSSFeed feed, boolean newItems) {
				if (newItems) {
					List<RSSItem> itemList = feed.getNewItems();
					Collections.sort(itemList, new Comparator<RSSItem>() {										
										public int compare (
												RSSItem o1,
												RSSItem o2) {
											int c = o1.getTitle().compareToIgnoreCase(o2.getTitle());
											if (c != 0) {
												return c;
											} else {
												return o1.getLink().compareTo(o2.getLink());
											}
										}
									});
					if (links.containsKey(feed.getId())) {
						Set<Filter> filterSet = links.get(feed.getId());
						for (Filter fil : filterSet) {
							for (RSSItem i : itemList) {
								synchronized (fil) {
									fil.apply(i);
								}
							}
						}
					}
				}
			}
			public void error (
					Exception e) {
			}
			public void error (
					String m) {
			}
			public void updating (
					RSSFeed feed) {
			}
		};

	private ActionProvider					actionProvider;

	public void reset () {
		feeds = new Vector<RSSFeed>();
		filter = new HashMap<Integer, Filter>();
		links = new HashMap<Integer, Set<Filter>>();
		for (RSSFeedManagerListener l : listeners) {
			l.reset();
		}
	}

	public RSSFeedManager(ActionProvider ap) {
		actionProvider = ap;
	}

	/**
	 * @param actionProvider the actionProvider to set
	 */
	public void setActionProvider (ActionProvider actionProvider) {
		this.actionProvider = actionProvider;
	}

	/**
	 * @return the actionProvider
	 */
	public ActionProvider getActionProvider () {
		return actionProvider;
	}

	public void updateAllFeeds () {
		if (updating) {
			return;
		}
		Thread t = new Thread(new Runnable() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Runnable#run()
			 */
			public void run () {
				updating = true;
				for (RSSFeed f : feeds) {
					try {
						f.read();
					} catch (IOException e) {
						actionProvider.log("Update failed for Feed: "
								+ f.getName(), e);
						e.printStackTrace();
					}
				}
				updating = false;
			}
		});
		t.setPriority(Thread.MIN_PRIORITY);
		t.setDaemon(true);
		t.start();
	}

	public void updateAllFeedsAndRespectInterval () {
		if (updating) {
			return;
		}
		Thread t = new Thread(new Runnable() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Runnable#run()
			 */
			long	time	= System.currentTimeMillis() / 1000;

			public void run () {
				updating = true;
				for (RSSFeed f : feeds) {
					try {
						if (f.isFile()
								|| time
										- (f.getLastUpdate() + (f.getFailed() * 300)) >= f
										.getUpdateInterval()) {
							f.read();
						}
					} catch (IOException e) {
						f.failed();
						actionProvider.log("Update failed for Feed: "
								+ f.getName() + " [" + e.getMessage() + "]", e);
					}
				}
				updating = false;
			}
		});
		t.setPriority(Thread.MIN_PRIORITY);
		t.setDaemon(true);
		t.start();
	}

	/**
	 * @return the updating
	 */
	public boolean isUpdating () {
		return updating;
	}

	/**
	 * @return the proxy
	 */
	public Proxy getProxy () {
		return proxy;
	}

	/**
	 * @param proxy the proxy to use
	 */
	public void setProxy (Proxy proxy) {
		this.proxy = proxy;
	}

	public int getFeedCount () {
		return feeds.size();
	}

	public List<RSSFeed> getFeeds () {
		return feeds;
	}

	public Map<Integer, Filter> getFilter () {
		return filter;
	}

	public RSSFeed[] getFeedsAsArray () {
		return feeds.toArray(emptyFeedArray);
	}

	public void addFeed (RSSFeed f) {
		f.setRSSFeedManager(this);
		for (int i = 0; i < feeds.size(); i++) {
			if (feeds.get(i).getId() == f.getId()) {
				//prevent double id
				f.generateNewID();
				i = 0;
			}
		}
		feeds.add(f);
		Collections.sort(feeds);
		f.addListener(feedListener);

		for (RSSFeedManagerListener l : listeners) {
			l.feedAdded(f);
		}
	}

	public void removeFeed (RSSFeed f) {
		links.remove(f.getId());
		feeds.remove(f);
		for (RSSFeedManagerListener l : listeners) {
			l.feedRemoved(f);
		}
	}

	public void addFilter (Filter f) {
		f.setRSSFeedManager(this);
		while (filter.containsKey(f.getId())) {
			f.generateNewID();
		}
		filter.put(f.getId(), f);
		for (RSSFeedManagerListener l : listeners) {
			l.filterAdded(f);
		}
	}

	public void removeFilter (Filter f) {
		if (f != null) {
			for (int key : links.keySet()) {
				links.get(key).remove(f);
			}
			filter.remove(f.getId());
			for (RSSFeedManagerListener l : listeners) {
				l.filterRemoved(f);
			}
		}
	}

	public void assignFilterToFeed (Filter f, RSSFeed rf) {
		assignFilterToFeed(f, rf.getId(), false);
	}

	private void assignFilterToFeed (Filter f, int rf, boolean silent) {
		if (links.containsKey(rf)) {
			Set<Filter> l = links.get(rf);
			l.add(f);
		} else {
			Set<Filter> l = new HashSet<Filter>();
			l.add(f);
			links.put(rf, l);
		}
		if (!silent) {
			actionProvider.saveRSSData();
		}
	}

	public void assignFilterToAllFeeds (Filter f) {
		for (RSSFeed rf : feeds) {
			assignFilterToFeed(f, rf.getId(), true);
		}
		actionProvider.saveRSSData();
	}

	public void removeFilterFromAllFeeds (Filter f) {
		for (RSSFeed rf : feeds) {
			removeFilterFromFeed(f, rf.getId(), true);
		}
		actionProvider.saveRSSData();
	}

	public void addAllFiltersToFeed (RSSFeed rf) {
		if (!links.containsKey(rf.getId())) {
			links.put(rf.getId(), new HashSet<Filter>());
		}
		Set<Filter> l = links.get(rf.getId());

		for (Filter f : filter.values()) {
			l.add(f);
		}
	}

	public void removeAllFiltersFromFeed (RSSFeed rf) {
		links.remove(rf.getId());
	}

	public void removeFilterFromFeed (Filter f, RSSFeed rf) {
		removeFilterFromFeed(f, rf.getId(), false);
	}

	public void removeFilterFromFeed (Filter f, int rf, boolean silent) {
		if (links.containsKey(rf)) {
			Set<Filter> l = links.get(rf);
			l.remove(f);
			if (!silent) {
				actionProvider.saveRSSData();
			}
		}
	}

	public Filter[] getFilterAsArray () {
		return filter.values().toArray(emptyFilterArray);
	}

	public void addListener (RSSFeedManagerListener l) {
		listeners.add(l);
	}

	public void removeListener (RSSFeedManagerListener l) {
		listeners.remove(l);
	}

	/**
	 * @return the links
	 */
	public Map<Integer, Set<Filter>> getLinks () {
		return links;
	}

	public void clearAll () {
		listeners = new Vector<RSSFeedManagerListener>();
		feeds = new Vector<RSSFeed>();
		filter = new HashMap<Integer, Filter>();
		links = new HashMap<Integer, Set<Filter>>();
	}

	public void saveToFile (File f) throws IOException {
		OutputStream os = null;
		try {
			os = new FileOutputStream(f);
			if (f.toString().endsWith(".gz")) {
				os = new GZIPOutputStream(os);
			}
			saveToStream(os);
		} finally {
			if (os != null) {
				os.close();
			}
		}
	}

	public void saveToStream (OutputStream os) throws IOException {
		Document doc = new Document();
		doc.addContent(toElement());
		new XMLOutputter(Format.getPrettyFormat()).output(doc, System.out);
		new XMLOutputter(Format.getPrettyFormat()).output(doc, os);
	}

	public Element toElement () {
		Element root = new Element("RSSFeedManager");
		for (RSSFeed feed : feeds) {
			root.addContent(feed.toElement());
		}
		for (Filter fil : filter.values()) {
			root.addContent(fil.toElement());
		}
		Element linksElem = new Element("Links");
		for (int key : links.keySet()) {
			Element linkElem = new Element("Link");
			linkElem.setAttribute("feedID", Integer.toString(key));
			if (links.get(key) == null || links.get(key).size() == 0) {
				continue;
			}
			for (Filter fil : links.get(key)) {
				Element filter = new Element("Filter");
				filter.setAttribute("filterID", Integer.toString(fil.getId()));
				linkElem.addContent(filter);
			}
			linksElem.addContent(linkElem);
		}
		root.addContent(linksElem);
		return root;
	}

	public void loadFromStream (InputStream is) throws IOException {
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(is);
			readFromDoc(doc);
		} catch (JDOMException e) {
			e.printStackTrace();
		}
	}

	public void loadFromFile (File f) throws IOException {
		InputStream is = null;
		try {
			is = new FileInputStream(f);
			if (f.toString().endsWith(".gz")) {
				is = new GZIPInputStream(is);
			}
			loadFromStream(is);
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public void readFromDoc (Document doc) {
		readFromElement(doc.getRootElement());
	}

	@SuppressWarnings("unchecked")
	public void readFromElement (Element root) {
		actionProvider.disableSave(true);
		List<Element> elems = root.getChildren(RSSFeed.getElementName());
		for (Element e : elems) {
			addFeed(new RSSFeed(e));
		}
		elems = root.getChildren(FileType.getElementName());
		for (Element e : elems) {
			fileTypes.add(new FileType(e));
		}
		elems = root.getChildren(FileType.getElementName());
		for (Element e : elems) {
			int fileTypeID = Integer.parseInt(e.getAttributeValue("id"));
			List<Element> subtypes = e.getChildren("sub");
			Set<FileType> subtypeSet = new HashSet<FileType>();
			for (Element sub : subtypes) {
				int subID = Integer.parseInt(sub.getAttributeValue("refID"));
				subtypeSet.add(fileTypes.get(subID));
			}
			if (subtypes.size() > 0) {
				fileTypes.get(fileTypeID).setSubtypes(subtypeSet);
			}
		}

		elems = root.getChildren(Filter.getElementName());
		for (Element e : elems) {
			addFilter(new Filter(e));
		}
		List<Element> linkList = root.getChild("Links").getChildren();
		for (Element e : linkList) {
			try {
				int rfeed = e.getAttribute("feedID").getIntValue();
				List<Element> filterElems = e.getChildren();
				for (Element fe : filterElems) {
					Filter f = filter.get(Integer.parseInt(fe
							.getAttributeValue("filterID")));
					if (f == null) {
						continue;
					}
					assignFilterToFeed(f, rfeed, true);
				}
			} catch (DataConversionException e1) {
				e1.printStackTrace();
			}
		}
		actionProvider.disableSave(false);
	}
}
