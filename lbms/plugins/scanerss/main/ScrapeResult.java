package lbms.plugins.scanerss.main;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Damokles
 *
 */
public class ScrapeResult {
	private String scrapeUrl = "";
	private long seeds, leechers;

	private boolean failure = false;

	private String failureReason = "";

	public ScrapeResult (byte[] scrapeData) {
		try {
			String data = new String (scrapeData, "ISO-8859-1");
			if (data.contains("failure reason")) {
				failure = true;
				Pattern fr = Pattern.compile("14:failure reason\\d+:(.*?)\\d+:", Pattern.CASE_INSENSITIVE);
				Matcher m = fr.matcher(data);
				if (m.find()) {
					failureReason = m.group(1);
				}

			} else {
				Pattern cp = Pattern.compile(":completei(\\d)+e", Pattern.CASE_INSENSITIVE);
				Pattern icp = Pattern.compile(":incompletei(\\d)+e", Pattern.CASE_INSENSITIVE);
				Matcher m = cp.matcher(data);
				if (m.find()) {
					seeds = Long.parseLong(m.group(1));
				}
				m = icp.matcher(data);
				if (m.find()) {
					leechers = Long.parseLong(m.group(1));
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public ScrapeResult (String scrapeURL, byte[] scrapeData) {
		this(scrapeData);
		this.scrapeUrl = scrapeURL;
	}

	/**
	 * @return Returns the leechers.
	 */
	public long getLeechers() {
		return leechers;
	}

	/**
	 * @return Returns the seeds.
	 */
	public long getSeeds() {
		return seeds;
	}

	/**
	 * @return Returns the failure.
	 */
	public boolean hasFailed() {
		return failure;
	}

	/**
	 * @return Returns the failureReason.
	 */
	public String getFailureReason() {
		return failureReason;
	}

	/**
	 * @return Returns the scrapeUrl.
	 */
	public String getScrapeUrl() {
		return scrapeUrl;
	}
}
