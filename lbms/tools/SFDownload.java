package lbms.tools;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Damokles
 *
 */
public class SFDownload extends Download {

	private static Pattern mirrorPattern = Pattern.compile("<td><a href=\"[\\w/:.]+?\\?use_mirror=(\\w+)\"><b>Download</b></a></td>");
	private List<URL> mirrors = new ArrayList<URL>();
	private HTTPDownload currentDL;

	/**
	 * This listener is used to pass the underlying HTTPDownload
	 * status to the caller.
	 */
	private DownloadListener dlL = new DownloadListener() {
		public void progress(long bytesRead, long bytesTotal) {
			callProgress(bytesRead, bytesTotal);
		}

		public void stateChanged(int oldState, int newState) {
			if (newState == STATE_CONNECTING || newState == STATE_DOWNLOADING)
				callStateChanged(newState);
		}
		/* (non-Javadoc)
		 * @see lbms.tools.DownloadListener#debugMsg(java.lang.String)
		 */
		public void debugMsg(String msg) {
			// TODO Auto-generated method stub

		}
	};

	public SFDownload(URL source, File target) {
		super(source, target);
		// TODO Auto-generated constructor stub
	}

	public SFDownload(URL source) {
		throw new UnsupportedOperationException("This Download needs a target.");
	}

	public void run() {
		try {
			call();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Download call() throws Exception {
		currentDL = new HTTPDownload(this);
		currentDL.setSource(source);
		currentDL.setTarget(null); //we want to have the buffer only
		if (proxy != null);
			currentDL.setProxy(proxy);
		try {
			callStateChanged(STATE_INITIALIZING);
			currentDL.call();
		} catch (Exception e1) {
			e1.printStackTrace();
			callStateChanged(STATE_FAILURE);
			failureReason = e1.getMessage();
			failed = true;
			throw e1;
		}
		if (currentDL.hasFailed() || !currentDL.hasFinished()) {
			callStateChanged(STATE_FAILURE);
			failureReason = "Couldn't load mirrors";
			failed = true;
			System.out.println(failureReason);
			throw new Exception ("SFDownload Error occured:"+failureReason);
		}
		Matcher sfMirror = mirrorPattern.matcher(currentDL.getBuffer().toString());
		String fileLocation = source.getPath();
		//Find mirrors
		while (sfMirror.find()) {
			String mirrorId = sfMirror.group(1);
			try {
				mirrors.add(new URL("http://"+mirrorId+".dl.sourceforge.net/sourceforge"+fileLocation));
				//System.out.println("SF.net Mirror: http://"+mirrorId+".dl.sourceforge.net/sourceforge"+fileLocation);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		String referer = source.toExternalForm();
		String cookie = currentDL.getCookie();
		//Downloading file
		for (URL x:mirrors) {
			try {
				//System.out.println("Trying: "+x.toExternalForm());
				if (abort) {
					callStateChanged(STATE_ABORTED);
					failed = true;
					failureReason = "Aborted by User";
					return this;
				}
				currentDL = new HTTPDownload(this);
				currentDL.setSource(x);
				currentDL.setCookie(cookie);
				if (proxy != null);
					currentDL.setProxy(proxy);
				currentDL.setReferer(referer);
				currentDL.addDownloadListener(dlL);
				currentDL.call();
				currentDL.removeDownloadListener(dlL);
				if (currentDL.hasFailed() || !currentDL.hasFinished()) continue;
				else {
					callStateChanged(STATE_FINISHED);
					return this;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//I hope we are not getting here
		callStateChanged(STATE_FAILURE);
		failureReason = "Couldn't Download file";
		failed = true;
		System.out.println(failureReason);
		throw new Exception (failureReason);
	}

	/* (non-Javadoc)
	 * @see lbms.tools.Download#abortDownload()
	 */
	@Override
	public void abortDownload() {
		super.abortDownload();
		if (currentDL != null) currentDL.abortDownload();
	}
}
