package lbms.plugins.scanerss.main;

import java.io.PrintStream;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import org.jdom.DataConversionException;
import org.jdom.Element;

/**
 * @author Damokles
 * 
 */
public class DownloadHistory {

	private int						minSeason, minEp;
	private int						maxSeason, maxEp;
	private List<List<Integer>>		seasonList		= new Vector<List<Integer>>();
	private Pattern					customEpPattern;
	private EpisodePatternMatching	epPatternMode	= EpisodePatternMatching.Normal;
	private boolean					downloadHigherVersion;

	public DownloadHistory(int minSeason, int minEp, int maxSeason, int maxEp) {
		this.minSeason = minSeason;
		this.maxSeason = maxSeason;
		this.minEp = minEp;
		this.maxEp = maxEp;
	}

	@SuppressWarnings("unchecked")
	public DownloadHistory(Element e) {
		minSeason = Integer.parseInt(e.getAttributeValue("minSeason"));
		maxSeason = Integer.parseInt(e.getAttributeValue("maxSeason"));
		minEp = Integer.parseInt(e.getAttributeValue("minEp"));
		maxEp = Integer.parseInt(e.getAttributeValue("maxEp"));
		downloadHigherVersion = Boolean.parseBoolean(e
				.getAttributeValue("higherVersion"));
		List<Element> seasons = e.getChildren("Season");
		for (Element se : seasons) {
			List<Integer> episodes = new Vector<Integer>();
			String cont = se.getTextTrim();
			for (int i = 0; i < cont.length(); i++) {
				episodes.add(i, Integer.parseInt(Character.toString(cont
						.charAt(i))));
			}
			try {
				fillSeasonList(se.getAttribute("id").getIntValue());
				seasonList.set(se.getAttribute("id").getIntValue(), episodes);
			} catch (DataConversionException e1) {
				e1.printStackTrace();
			}
		}
		String pat = e.getAttributeValue("customEpPattern");
		if (pat != null) {
			customEpPattern = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
		}
		try {
			epPatternMode = EpisodePatternMatching.valueOf(e
					.getAttributeValue("epPatternMode"));
		} catch (Exception e1) {
			epPatternMode = EpisodePatternMatching.Normal;
		}
	}

	/**
	 * Checks if an Episode should be downloaded
	 * 
	 * @param e Episode to check
	 * @return true / false
	 */
	public boolean check (Episode e) {
		if (minSeason > e.getSeason_end()) {
			return false;
		}
		if (minSeason == e.getSeason_end() && minEp > e.getEpisode_end()) {
			return false;
		}

		if (maxSeason != -1) {
			if (maxSeason < e.getSeason_end()) {
				return false;
			}
			if (maxSeason == e.getSeason_end() && maxEp != -1
					&& maxEp < e.getEpisode_end()) {
				return false;
			}
		}

		if (e.getSeason() == e.getSeason_end()) { //Same Season only compare episodes
			if (seasonList.size() > e.getSeason() //checks if there is any info for this season
					&& seasonList.get(e.getSeason()) != null) {
				if (e.getEpisode() == e.getEpisode_end()) { //checks if start and end of the ep are the same, meaning 1 Episode
					return !checkEp(e.getSeason(), e.getEpisode(), e
							.getVersion());
				} else if (e.getEpisode() <= e.getEpisode_end()) { //checks if there are more than one episode
					for (int i = e.getEpisode(); i <= e.getEpisode_end(); i++) {
						if (checkEp(e.getSeason(), i, e.getVersion())) {
							return false;
						}
					}
					return true; // no eps where present in the downloadhistory
				}
				return false; // failure
			} else {
				return true; // there was no info for this season so it is not in the history
			}
		} else if (e.getSeason() < e.getSeason_end()) { //checks if this spans seasons
			if (seasonList.size() > e.getSeason_end() //checks if season list contains the episodes
					&& seasonList.get(e.getSeason()) != null
					&& seasonList.get(e.getSeason_end() - 1) != null) {
				if (checkEp(e.getSeason(), e.getEpisode(), e.getVersion())) {
					return false;
				}
				for (int i = 0; i < e.getEpisode_end(); i++) {
					if (checkEp(e.getSeason(), i, e.getVersion())) {
						return false;
					}
				}
				return true;
			} else {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds an Episode to the Downloaded List
	 * 
	 * @param e Episode to add
	 */
	public void add (Episode e) {
		fillSeasonList(e.getSeason());
		fillSeasonList(e.getSeason_end());
		if (e.getSeason() == e.getSeason_end()) {
			if (e.getEpisode() == e.getEpisode_end()) {
				// add episode
				setEpisode(e.getSeason(), e.getEpisode(), e.getVersion());

			} else if (e.getEpisode() <= e.getEpisode_end()) {
				for (int i = e.getEpisode(); i < e.getEpisode_end(); i++) {
					// add all episodes: ep - ep_end
					setEpisode(e.getSeason(), i, e.getVersion());
				}
			}

		} else if (e.getSeason() < e.getSeason_end()) {
			if (seasonList.size() < e.getSeason()
					|| seasonList.get(e.getSeason()) == null) {
				fillSeasonList(e.getSeason());
			}
			// add last episode of first season
			setEpisode(e.getSeason(), e.getEpisode(), e.getVersion());

			for (int i = 0; i < e.getEpisode_end(); i++) {
				// add all episodes from 2nd season ep 1 - ep_end
				setEpisode(e.getSeason_end(), i, e.getVersion());
			}
		}
	}

	/**
	 * Fills the Season List until s is reached
	 * 
	 * @param s Season to fill
	 */
	private void fillSeasonList (int s) {
		if (seasonList.size() > s) {
			if (seasonList.get(s) == null) {
				seasonList.set(s, new Vector<Integer>());
			}
		} else {
			for (int i = seasonList.size(); i < s; i++) {
				seasonList.add(null);
			}
			seasonList.add(new Vector<Integer>());
		}
	}

	/**
	 * Adds an Episode to the Downloaded List
	 * 
	 * @param s Season
	 * @param e Episode
	 * @param v Version
	 */
	private void setEpisode (int s, int e, int v) {
		fillSeasonList(s);
		if (seasonList.get(s).size() > e) {
			seasonList.get(s).set(e, v);
		} else {
			for (int i = seasonList.get(s).size(); i < e; i++) {
				seasonList.get(s).add(0);
			}
			seasonList.get(s).add(v);
		}
	}

	/**
	 * Removes the Episode from the Downloaded list
	 * 
	 * @param s Season
	 * @param e Episode
	 */
	private void unsetEpisode (int s, int e) {
		if (seasonList.size() > s && seasonList.get(s).size() > e) {
			seasonList.get(s).set(e, 0);
		}
	}

	/**
	 * Checks whether an Episode was already Downloaded
	 * 
	 * @param s Season
	 * @param e Episode
	 * @param v Version
	 * @return true if already downloaded, false otherwise
	 */
	private boolean checkEp (int s, int e, int v) {
		fillSeasonList(s);
		if (seasonList.get(s).size() <= e) {
			return false;
		} else {
			if (downloadHigherVersion) {
				return seasonList.get(s).get(e) >= v;
			} else {
				return seasonList.get(s).get(e) == 1;
			}
		}
	}

	public boolean checkAndAdd (Episode e) {
		if (check(e)) {
			add(e);
			return true;
		}
		return false;
	}

	public void remove (Episode e) {
		if (e.getSeason() == e.getSeason_end()) {
			if (e.getEpisode() == e.getEpisode_end()) {
				unsetEpisode(e.getSeason(), e.getEpisode());

			} else if (e.getEpisode() <= e.getEpisode_end()) {
				for (int i = e.getEpisode(); i < e.getEpisode_end(); i++) {
					unsetEpisode(e.getSeason(), i);
				}
			}

		} else if (e.getSeason() < e.getSeason_end()) {
			for (int i = 0; i < e.getEpisode_end(); i++) {
				unsetEpisode(e.getSeason_end(), i);
			}
		}
	}

	/**
	 * @return the XML Element name
	 */
	public static String getElementName () {
		return "DownloadHistory";
	}

	/**
	 * Serializes DownloadHistory into an XML Element
	 * 
	 * @return
	 */
	public Element toElement () {
		Element e = new Element(getElementName());
		e.setAttribute("minSeason", Integer.toString(minSeason));
		e.setAttribute("maxSeason", Integer.toString(maxSeason));
		e.setAttribute("minEp", Integer.toString(minEp));
		e.setAttribute("maxEp", Integer.toString(maxEp));
		e.setAttribute("epPatternMode", epPatternMode.name());
		if (customEpPattern != null) {
			e.setAttribute("customEpPattern", customEpPattern.pattern());
		}
		if (downloadHigherVersion) {
			e.setAttribute("higherVersion", "true");
		}

		for (List<Integer> l : seasonList) {
			if (l != null) {
				Element season = new Element("Season");
				season.setAttribute("id", Integer.toString(seasonList
						.indexOf(l)));
				StringBuffer sb = new StringBuffer(l.size());
				for (Integer i : l) {
					sb.append(i);

				}
				season.setText(sb.toString());
				e.addContent(season);
			}
		}
		return e;
	}

	/**
	 * Don't modify this List!!!
	 * 
	 * @return the seasonList
	 */
	public List<List<Integer>> getSeasonList () {
		return seasonList;
	}

	/**
	 * @return the maxEp
	 */
	public int getMaxEp () {
		return maxEp;
	}

	/**
	 * @param maxEp the maxEp to set
	 */
	public void setMaxEp (int maxEp) {
		this.maxEp = maxEp;
	}

	/**
	 * @return the maxSeason
	 */
	public int getMaxSeason () {
		return maxSeason;
	}

	/**
	 * @param maxSeason the maxSeason to set
	 */
	public void setMaxSeason (int maxSeason) {
		this.maxSeason = maxSeason;
	}

	/**
	 * @return the minEp
	 */
	public int getMinEp () {
		return minEp;
	}

	/**
	 * @param minEp the minEp to set
	 */
	public void setMinEp (int minEp) {
		this.minEp = minEp;
	}

	/**
	 * @return the minSeason
	 */
	public int getMinSeason () {
		return minSeason;
	}

	/**
	 * @param minSeason the minSeason to set
	 */
	public void setMinSeason (int minSeason) {
		this.minSeason = minSeason;
	}

	/**
	 * @return the customEpPattern
	 */
	public Pattern getCustomEpPattern () {
		return customEpPattern;
	}

	/**
	 * @param customEpPattern the customEpPattern to set
	 */
	public void setCustomEpPattern (Pattern customEpPattern) {
		this.customEpPattern = customEpPattern;
	}

	/**
	 * @return the epPatternMode
	 */
	public EpisodePatternMatching getEpPatternMode () {
		return epPatternMode;
	}

	/**
	 * @param epPatternMode the epPatternMode to set
	 */
	public void setEpPatternMode (EpisodePatternMatching epPatternMode) {
		this.epPatternMode = epPatternMode;
		if (epPatternMode.equals(EpisodePatternMatching.Normal)) {
			customEpPattern = null;
		}
	}

	/**
	 * @return the downloadHigherVersion
	 */
	public boolean isDownloadHigherVersion () {
		return downloadHigherVersion;
	}

	/**
	 * @param downloadHigherVersion the downloadHigherVersion to set
	 */
	public void setDownloadHigherVersion (boolean downloadHigherVersion) {
		this.downloadHigherVersion = downloadHigherVersion;
	}

	public void printToStream (PrintStream os) {
		os.println("DownloadHistory");
		for (List<Integer> l : seasonList) {
			if (l != null) {
				os.println("\tSeason: " + seasonList.indexOf(l));
				os.print('\t');
				for (Integer i : l) {
					os.append(i.toString());
				}
				os.println();
			}
		}
		os.println();
	}

	/**
	 * This enum defines the Episode Pattern Matching mode.
	 * 
	 * Normal - use built in Patterns for episode matching others - use self
	 * defined Pattern for episode matching and treat groups as statet in the
	 * name
	 * 
	 * @author Damokles
	 */
	public enum EpisodePatternMatching {
		Normal, Episode, Episode_Episode, Season_Episode, Season_Episode_Episode, Season_Episode_Season_Episode
	}
}
