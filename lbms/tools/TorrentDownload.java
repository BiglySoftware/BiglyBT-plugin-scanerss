package lbms.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Damokles
 * 
 */
public class TorrentDownload extends Download {

	public static final int					RTC_ERROR					= -1;
	public static final int					RTC_INIT					= 0;
	public static final int					RTC_FILE					= 1;
	public static final int					RTC_BUFFER					= 2;
	public static final int					RTC_MAGNET					= 3;

	private static final int				BUFFER_SIZE					= 1024;

	private static HttpURLConnectionFactory	defaultConnectionFactory	= new DefaultHttpURLConnectionFactory();

	private ByteArrayOutputStream			buffer;

	private static final String				torrentContentType			= "application/x-bittorrent";

	private static Pattern					hrefPattern					= Pattern
																				.compile(
																						"href\\s*=\\s*(?:\"|')([\\w:/.?&-=%\\[\\]{}\\(\\) ]+)(?:\"|')",
																						Pattern.CASE_INSENSITIVE);

	private static Pattern					torrentHrefPattern			= Pattern
																				.compile(
																						"href\\s*=\\s*(?:\"|')([\\w:/.?&-=%\\[\\]{}\\(\\) ]+\\.torrent)(?:\"|')",
																						Pattern.CASE_INSENSITIVE);
	private static Pattern					magnetPattern				= Pattern
																				.compile(
																						"magnet:\\?xt=urn:btih:[A-Za-z0-9]{32}",
																						Pattern.CASE_INSENSITIVE);
	private static Pattern					magnetPattern64				= Pattern
																				.compile(
																						"magnet:\\?xt=urn:btih:[A-Za-z0-9]{40}",
																						Pattern.CASE_INSENSITIVE);

	private static Pattern					torrentDataPattern			= Pattern
																				.compile(
																						"^d[0-9]+:.*",
																						Pattern.CASE_INSENSITIVE
																								| Pattern.DOTALL);

	private static String[]					torrentLinkLikelines		= new String[] {
			"get", "down", "download", "torrent"						};

	private String							torrentLinkIdentifier;

	private int								returnCode					= RTC_INIT;

	private String							magnetURL;

	private boolean							isXHTML						= false;

	private HttpURLConnectionFactory		connectionFactory			= defaultConnectionFactory;

	/**
	 *
	 */
	public TorrentDownload() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param source
	 */
	public TorrentDownload(URL source) {
		super(source);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param source
	 * @param target
	 */
	public TorrentDownload(URL source, File target) {
		super(source, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param d
	 */
	public TorrentDownload(Download d) {
		super(d);
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run () {
		try {
			call();
		} catch (Exception e) {
			debugMsg(e.getMessage());
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	public Download call () throws Exception {
		try {
			/* ################### Initialise Connection ################# */
			debugMsg("TorrentDownloader: starting Download [" + source + "]");
			HTTPDownload httpDownload = new HTTPDownload(source);
			//redirect all logging here
			httpDownload.addDownloadListener(new DownloadListener() {

				@Override
				public void debugMsg (String msg) {
					debugMsg(msg);

				}

				@Override
				public void progress (long bytesRead, long bytesTotal) {
					callProgress(bytesRead, bytesTotal);

				}

				@Override
				public void stateChanged (int oldState, int newState) {
					callStateChanged(newState);
				}
			});
			httpDownload.call();

			/* ################### Analyse Content ################# */
			buffer = httpDownload.getBuffer();

			if (httpDownload.failed) {
				failed = httpDownload.failed;
				failureReason = httpDownload.failureReason;
				callStateChanged(STATE_FAILURE);
			} else {
				if (isTorrent(buffer.toByteArray())) {
					if (target != null) {
						target.createNewFile();
						FileOutputStream os = null;
						try {
							os = new FileOutputStream(target);
							buffer.writeTo(os);
						} finally {
							if (os != null) {
								os.close();
							}
						}
						returnCode = RTC_FILE;
					} else {
						returnCode = RTC_BUFFER;
					}
					debugMsg("TorrentDownloader: Download Successful ["
							+ source + "]");
					finished = true;
					callStateChanged(STATE_FINISHED);
				} else {
					//The downloaded Data was not a torrent
					//assume that it was html

					debugMsg("TorrentDownloader: parsing HTML for link ["
							+ source + "]");
					try {
						String html = buffer.toString("UTF-8");
						if (html.substring(0, 100).toLowerCase().contains(
								"xhtml")) {
							isXHTML = true;
						}

						if (findTorrentLink(html)) {
							//findTorrentLink resets the target url so we need to call again.
							return call();
						}
						if (findMagnetLink(html)) {
							return this;
						}

						if (findTorrentByIdentifier(html)) {
							return call();
						}

						debugMsg("TorrentDownloader: parsing all links for torrent ["
								+ source + "]");

						//no direct torrent or magnet link was found
						if (findTorrentBySearchingAllLinks(html)) {
							return call();

						}

						debugMsg("TorrentDownloader: failed to find torrent Link ["
								+ source + "]");

						failed = true;
						failureReason = "No Torrent Data/Link found";
						callStateChanged(STATE_FAILURE);

					} catch (Exception e) {
						e.printStackTrace();
						failed = true;
						failureReason = e.getMessage();
						callStateChanged(STATE_FAILURE);
					}
				}
			}
		} catch (IOException e) {
			failed = true;
			failureReason = e.getMessage();
			callStateChanged(STATE_FAILURE);
			e.printStackTrace();
			throw e;
		} finally {
		}
		return this;
	}

	private boolean findTorrentLink (String html) {
		Matcher tor = torrentHrefPattern.matcher(html);
		if (tor.find()) {
			try {
				String torlink = tor.group(1);
				if (isXHTML) {
					torlink = torlink.replace("&amp;", "&");
				}
				//set new source and download again
				URL torURL = resolveRelativeURL(source, torlink);

				debugMsg("TorrentDownloader: found Torrent Link [" + torURL
						+ "]");
				if (isHrefTorrent(torURL)) {
					source = torURL;
					callStateChanged(STATE_RESET);
					return true;
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private boolean findMagnetLink (String html) {
		Matcher magnet64 = magnetPattern64.matcher(html);
		if (magnet64.find()) {
			magnetURL = magnet64.group();
			returnCode = RTC_MAGNET;
			finished = true;
			debugMsg("TorrentDownloader: found Magnet Link [" + magnetURL
					+ "] from [" + source + "]");
			callStateChanged(STATE_FINISHED);
			return true;
		}
		Matcher magnet = magnetPattern.matcher(html);
		if (magnet.find()) {
			magnetURL = magnet.group();
			returnCode = RTC_MAGNET;
			finished = true;
			debugMsg("TorrentDownloader: found Magnet Link [" + magnetURL
					+ "] from [" + source + "]");
			callStateChanged(STATE_FINISHED);
			return true;
		}
		return false;
	}

	private boolean findTorrentByIdentifier (String html) {
		if (torrentLinkIdentifier != null) {
			debugMsg("TorrentDownloader: trying to use torrentLinkIdentifier ["
					+ torrentLinkIdentifier + "]");
			Matcher m = Pattern.compile(
					"href\\s*=\\s*(?:\"|')([\\w:/.?&-=%\\[\\]{}\\(\\) ]*"
							+ Pattern.quote(torrentLinkIdentifier)
							+ "[\\w:/.?&-=%\\[\\]{}\\(\\) ]*)(?:\"|')",
					Pattern.CASE_INSENSITIVE).matcher(html);
			if (m.find()) {
				try {
					String torlink = m.group(1);
					//set new source and download again
					URL torURL = resolveRelativeURL(source, torlink);
					debugMsg("TorrentDownloader: found Torrent Link [" + torURL
							+ "]");
					if (isHrefTorrent(torURL)) {
						source = torURL;
						callStateChanged(STATE_RESET);
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			} else {
				debugMsg("TorrentDownloader: failed to find torrent by torrentLinkIdentifier ["
						+ torrentLinkIdentifier + "]");
			}

		}
		return false;
	}

	private boolean findTorrentBySearchingAllLinks (String html) {
		Matcher links = hrefPattern.matcher(html);
		List<LinkAndURL> urls = new ArrayList<LinkAndURL>();
		while (links.find()) {
			try {
				String torlink = links.group(1);
				if (isXHTML) {
					torlink = torlink.replace("&amp;", "&");
				}
				URL torURL = resolveRelativeURL(source, torlink);
				urls.add(new LinkAndURL(torlink, torURL));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		//sort all links an move the links that might be torrents to the beginning of the list
		Collections.sort(urls, new Comparator<LinkAndURL>() {
			@Override
			public int compare (LinkAndURL o1, LinkAndURL o2) {
				int rating1 = rateTorrentLinkLikelines(o1.getUrl());
				int rating2 = rateTorrentLinkLikelines(o2.getUrl());
				if (rating1 == rating2) {
					return o1.getUrl().toExternalForm().compareTo(
							o2.getUrl().toExternalForm());
				} else if (rating1 > rating2) {
					//we return -1 here because it sorts to the beginning of the list
					return -1;
				}
				return 1;
			}
		});

		for (LinkAndURL torURL : urls) {

			if (isHrefTorrent(torURL.getUrl())) {
				setTorrentLinkIdentifier(torURL.getLink());
				source = torURL.getUrl();
				callStateChanged(STATE_RESET);
				return true;
			}
		}
		return false;
	}

	private boolean isHrefTorrent (URL target) {
		try {
			HttpURLConnection conn = connectionFactory.getConnection(target,
					proxy);
			if (target.toExternalForm().contains("?page=download")) {
				int x = 0;
				x = x + 1;
			}

			conn.setRequestMethod("HEAD");
			conn.connect();
			String contentType = conn.getContentType();
			conn.disconnect();
			if (contentType != null) {
				if (contentType.toLowerCase().startsWith(torrentContentType)) {
					return true;
				}
			}

			String contentDisposition = conn
					.getHeaderField("content-disposition");
			if (contentDisposition != null) {
				if (contentDisposition.toLowerCase().contains(".torrent")) {
					return true;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected static URL resolveRelativeURL (URL u, String href)
			throws MalformedURLException {
		/*
		 * String newUrl = u.getProtocol() + "://" + u.getHost(); if(u.getPort()
		 * > 0) newUrl += ":" + u.getPort(); if(!href.startsWith("/")) { // path
		 * relative to current String path = u.getPath(); // e.g /dir/file.php
		 * if(path.indexOf("/") > -1) path = path.substring(0,
		 * path.lastIndexOf("/") + 1); // strip file part newUrl += path; //
		 * append /dir if(!newUrl.endsWith("/")) newUrl += "/"; }
		 */
		return new URL(u, href);
	}

	/**
	 * @return Returns the buffer.
	 */
	public ByteArrayOutputStream getBuffer () {
		return buffer;
	}

	public boolean isTorrent (byte[] x) {
		//we only need the 10 fist chars
		int length = x.length < 10 ? x.length : 10;
		byte[] y = new byte[length];
		System.arraycopy(x, 0, y, 0, length);

		try {
			if (torrentDataPattern.matcher(new String(y, "UTF-8")).find()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public boolean isTorrent (File x) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(x);

			//we only need the 10 fist chars
			byte[] header = new byte[10];
			fis.read(header);

			if (torrentDataPattern.matcher(new String(header, "UTF-8")).find()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}

		return false;
	}

	/**
	 * This method is used for sorting the links that might be torrents at the
	 * beginning of the list.
	 * 
	 * @param link
	 * @return
	 */
	public int rateTorrentLinkLikelines (URL link) {
		String url = link.toExternalForm().toLowerCase();
		int rating = 0;
		for (int i = 0; i < torrentLinkLikelines.length; i++) {
			if (url.contains(torrentLinkLikelines[i])) {
				rating += i + 1;
			}
		}
		if (url.contains(source.getHost().toLowerCase())) {
			rating += torrentLinkLikelines.length + 10;
		}

		//System.out.println("Rating: " + rating + " for: " + link);
		return rating;
	}

	/**
	 * @return the magnetURL
	 */
	public String getMagnetURL () {
		return magnetURL;
	}

	/**
	 * @return the RTC_* returnCode
	 */
	public int getReturnCode () {
		return returnCode;
	}

	/**
	 * @return the torrentLinkIdentifier
	 */
	public String getTorrentLinkIdentifier () {
		return torrentLinkIdentifier;
	}

	public void setTorrentLinkIdentifier (String tor) {
		if (torrentLinkIdentifier == null) {
			torrentLinkIdentifier = tor;
		} else {
			if (torrentLinkIdentifier.equalsIgnoreCase(tor)) {
				return;
			}
			int i = 0;
			for (; i < tor.length() && i < torrentLinkIdentifier.length()
					&& tor.charAt(i) == torrentLinkIdentifier.charAt(i); i++) {
				;
			}
			if (i == 0) {
				torrentLinkIdentifier = null;
			} else {
				torrentLinkIdentifier = torrentLinkIdentifier.substring(0, i);
				debugMsg("TorrentDownloader: improved torrentLinkIdentifier: "
						+ torrentLinkIdentifier);
			}
		}
	}

	/**
	 * @param connectionFactory the connectionFactory to set
	 */
	public void setConnectionFactory (HttpURLConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * @param defaultConnectionFactory the defaultConnectionFactory to set
	 */
	public static void setDefaultConnectionFactory (
			HttpURLConnectionFactory defaultConnectionFactory) {
		TorrentDownload.defaultConnectionFactory = defaultConnectionFactory;
	}

	protected class LinkAndURL {
		private String	link;
		private URL		url;

		public LinkAndURL(String link, URL url) {
			super();
			this.link = link;
			this.url = url;
		}

		public String getLink () {
			return link;
		}

		public URL getUrl () {
			return url;
		}
	}
}
