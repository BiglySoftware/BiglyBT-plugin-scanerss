package lbms.tools;

import java.io.File;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.codec.binary.Base64;

/**
 * Abstract Class for Downloads.
 *
 * Requires Commons-Codec.jar {@link http://jakarta.apache.org/commons/codec/}
 *
 *
 * @author Damokles
 *
 */
public abstract class Download implements Runnable, Callable<Download> {

	public static final int TIMEOUT = 30000;

	public static final int STATE_ABORTED 		= -2;
	public static final int STATE_FAILURE 		= -1;
	public static final int STATE_WAITING 		= 0;
	public static final int STATE_INITIALIZING 	= 1;
	public static final int STATE_CONNECTING 	= 2;
	public static final int STATE_DOWNLOADING 	= 3;
	public static final int STATE_FINISHED 		= 4;
	public static final int STATE_RESET 		= 5;

	protected URL source;
	protected File target;
	protected String failureReason = "";
	protected boolean finished = false;
	protected boolean failed = false;

	protected String referer;
	protected String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.8.1.2) Gecko/20070219 Firefox/2.0.0.2";
	protected String cookie;
	protected String login;

	protected Proxy proxy;
	protected List<DownloadListener> dlListener = new Vector<DownloadListener>();

	/**
	 * The current State of the Download
	 */
	protected int state = 0;

	/**
	 * If abort is true the Download should stop
	 * Downloading, close all Streams and go into
	 * State ABORTED
	 */
	protected boolean abort = false;

	protected Download() {}

	/**
	 * Constructs a new Download.
	 *
	 * The contents will be stored in a StringBuffer
	 *
	 * @param source
	 */
	public Download(URL source) {
		this.source = source;
	}

	/**
	 * Constructs a new Download.
	 *
	 * The contents will be stored in the target File
	 *
	 * @param source URL do download from
	 * @param target File to write to
	 */
	public Download(URL source, File target) {
		this.source = source;
		this.target = target;
	}

	/**
	 * Clone Constructor
	 *
	 * @param d Download to clone
	 */
	public Download (Download d) {
		this.source = d.source;
		this.target = d.target;
		this.cookie = d.cookie;
		this.referer = d.referer;
		this.login = d.login;
		this.proxy = d.proxy;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(URL source) {
		this.source = source;
	}

	/**
	 * @param target the target to set
	 */
	public void setTarget(File target) {
		this.target = target;
	}

	/**
	 * Sets the Proxy to use
	 *
	 * @param proxy the proxy to use
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * Tries to abort the Download
	 */
	public void abortDownload () {
		abort = true;
	}

	public String  getFailureReason() {
		return failureReason;
	}

	/**
	 * @return Returns the source.
	 */
	public URL getSource() {
		return source;
	}

	/**
	 * @return Returns the target or null
	 */
	public File getTarget() {
		return target;
	}


	/**
	 * @return the referer or null
	 */
	public String getReferer() {
		return referer;
	}

	/**
	 * This needs to be set before the Download is executed
	 *
	 * @param referer the referer to set
	 */
	public void setReferer(String referer) {
		this.referer = referer;
	}

	/**
	 * @return the userAgent
	 */
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * This needs to be set before the Download is executed
	 *
	 * @param userAgent the userAgent to set
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * The Cookie data to send to the server.
	 *
	 * @param cookie the cookie to set
	 */
	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	/**
	 * Gets the Cookie.
	 *
	 * Will be overwritten if the Webserver returns a Cookie
	 * @return the cookie
	 */
	public String getCookie() {
		return cookie;
	}

	public void setLogin (String username, String pass) {
		login = new String(new Base64().encode((username+":"+pass).getBytes()));
	}

	/**
	 * @return the state
	 */
	public int getState() {
		return state;
	}

	/**
	 * @return Returns the failed.
	 */
	public boolean hasFailed() {
		return failed;
	}

	/**
	 * @return Returns the finished.
	 */
	public boolean hasFinished() {
		return finished;
	}

	public void addDownloadListener (DownloadListener l) {
		dlListener.add(l);
	}

	public void removeDownloadListener (DownloadListener l) {
		dlListener.remove(l);
	}

	protected void callProgress (long bytesRead, long bytesTotal) {
		for (int i=0;i<dlListener.size();i++) {
			dlListener.get(i).progress(bytesRead, bytesTotal);
		}
	}

	protected void callStateChanged (int newS) {
		for (int i=0;i<dlListener.size();i++) {
			dlListener.get(i).stateChanged(state, newS);
		}
		state = newS;
	}

	protected void debugMsg (String msg) {
		for (int i=0;i<dlListener.size();i++) {
			dlListener.get(i).debugMsg(msg);
		}
	}
}
