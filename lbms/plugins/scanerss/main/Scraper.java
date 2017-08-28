package lbms.plugins.scanerss.main;

import java.net.URL;

import lbms.tools.HTTPDownload;

/**
 * @author Damokles
 *
 */
public class Scraper {
	public static ScrapeResult scrapeTorrent (ITorrent torrent) {
		String scrapeURL = torrent.getAnnounceUrl();
		if (scrapeURL.lastIndexOf('/') != scrapeURL.indexOf("announce")-1 ) return null;
		char first_separator = scrapeURL.indexOf('?') == -1 ? '?' : '&';
		scrapeURL = scrapeURL.replace("announce", "scrape");
		scrapeURL += first_separator+"info_hash="+torrent.getInfoHash()+"&peer_id=Scraper_"+Long.toString(System.currentTimeMillis()%1000000000000l);
//		System.out.println(scrapeURL);
		try {
			URL realScrapeURL = new URL (scrapeURL);
			HTTPDownload dl = new HTTPDownload(realScrapeURL);
			dl.run();
			if (dl.hasFailed()) {
				return null;
			} else {
				byte[] scrapeRes = dl.getBuffer().toString().getBytes("ISO-8859-1");
				return new ScrapeResult(scrapeURL,scrapeRes);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
