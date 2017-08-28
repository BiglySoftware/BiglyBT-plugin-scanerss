package lbms.plugins.scanerss.main;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lbms.plugins.scanerss.main.DownloadHistory.EpisodePatternMatching;

/**
 * @author Damokles
 * 
 */
public class Episode {

	private static List<Pattern>	patternList			= new ArrayList<Pattern>();
	static {
		patternList.add(Pattern.compile(
				"S(\\d{1,2}).*?E(\\d{1,3})\\W*S(\\d{1,2}).*?E(\\d{1,3})",
				Pattern.CASE_INSENSITIVE)); // S00E000 (-) S11E111
		patternList.add(Pattern.compile(
				"S(\\d{1,2}).*?E(\\d{1,3})(?:\\W+|\\W*?E)(\\d{1,3})",
				Pattern.CASE_INSENSITIVE)); // S00E000 (-) (E)111
		patternList.add(Pattern.compile("S(\\d{1,2}).*?E(\\d{1,3})",
				Pattern.CASE_INSENSITIVE)); // S00E000
		patternList.add(Pattern.compile(
				"(\\d{1,2})\\D(\\d{1,3})\\W+(\\d{1,2})\\D(\\d{1,3})",
				Pattern.CASE_INSENSITIVE)); // 00x000 (-) 11x111
		patternList.add(Pattern.compile(
				"(\\d{1,2})\\D(\\d{1,3})\\W+(\\d{1,3})",
				Pattern.CASE_INSENSITIVE)); // 00x000 (-) 111
		patternList.add(Pattern.compile("(\\d{1,2})[^\\d-](\\d{1,3})",
				Pattern.CASE_INSENSITIVE)); // 00x000
		patternList.add(Pattern.compile("E?(\\d{1,3})\\W+E?(\\d{1,3})",
				Pattern.CASE_INSENSITIVE)); // 000-111
		patternList.add(Pattern.compile("E?(\\d{1,3})",
				Pattern.CASE_INSENSITIVE)); // 000
	}

	private static Pattern			versionPattern		= Pattern.compile(
																"v(\\d)",
																Pattern.CASE_INSENSITIVE);

	private static Pattern			resolutionPattern	= Pattern.compile("(\\d{3,4})x(\\d{3,4})");

	private int						season				= 0;

	private int						season_end			= -1;

	private int						ep					= 0;

	private int						ep_end				= -1;

	private int						version				= 1;

	private ActionProvider			actionProvider;

	private EpisodePatternMatching	mode				= EpisodePatternMatching.Normal;

	private Pattern					customPattern;

	private boolean					lessThan100Episodes;

	public Episode (String s, ActionProvider ap, EpisodePatternMatching mode,
			Pattern customPattern) {
		this.mode = mode;
		this.customPattern = customPattern;
		this.actionProvider = ap;
		parseString(s);
	}

	public Episode (String s, ActionProvider ap) {
		this.actionProvider = ap;
		parseString(s);
	}

	public Episode (String s, EpisodePatternMatching mode, Pattern customPattern) {
		this.mode = mode;
		this.customPattern = customPattern;
		parseString(s);
	}

	public Episode (String s, ActionProvider ap, EpisodePatternMatching mode,
			Pattern customPattern, boolean lessThan100Episodes) {
		this.lessThan100Episodes = lessThan100Episodes;
		this.mode = mode;
		this.customPattern = customPattern;
		this.actionProvider = ap;
		parseString(s);
	}

	public Episode (String s, ActionProvider ap, boolean lessThan100Episodes) {
		this.lessThan100Episodes = lessThan100Episodes;
		this.actionProvider = ap;
		parseString(s);
	}

	public Episode (String s, EpisodePatternMatching mode,
			Pattern customPattern, boolean lessThan100Episodes) {
		this.lessThan100Episodes = lessThan100Episodes;
		this.mode = mode;
		this.customPattern = customPattern;
		parseString(s);
	}

	public Episode (String s) {
		parseString(s);
	}

	private void parseString (String s) {
		try {
			s = URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		{
			Matcher vm = versionPattern.matcher(s);
			if (vm.find()) {

				version = Integer.parseInt(vm.group(1));
				if (version < 1) {
					version = 1;
				}
			} else if (s.toLowerCase().contains("real.proper")) {
				version = 4;
			} else if (s.toLowerCase().contains("proper")) {
				version = 3;
			} else if (s.toLowerCase().contains("repack")) {
				version = 2;
			}
		}

		s = s.replaceAll("19\\d{2}|20\\d{2}", ""); // Remove 19xx and 20xx
		
		if (mode.equals(EpisodePatternMatching.Normal)) {
			// clean the string
			s = s.replaceAll("\\[.*?\\]|\\(.*?\\)|\\w?264|480p|576p|720p|1080p|v\\d", "");
			{
				Matcher resMatcher = resolutionPattern.matcher(s);
				if (resMatcher.find()) {
					int width = Integer.parseInt(resMatcher.group(1));
					int height = Integer.parseInt(resMatcher.group(2));
					if (width > 100
							&& ((width / 4 == height / 3)
									|| (width / 16 == height / 9) || (width / 16 == height / 10))) {
						s = s.replaceFirst(Integer.toString(width), "").replaceFirst(
								Integer.toString(height), "");
					}
				}
			}
			logMsg("Episode Checking: " + s);
			int i = 0;
			Matcher m = null;
			boolean found = false;
			for (; !found && i < patternList.size(); i++) {
				m = patternList.get(i).matcher(s);
				if (m.find()) {
					logMsg("Episode Matched: " + patternList.get(i));
					found = true;
				}
			}
			if (found) {
				if (i <= 6) {
					switch (m.groupCount()) {
					case 2:
						season = Integer.parseInt(m.group(1));
						ep = Integer.parseInt(m.group(2));
						break;
					case 3:
						season = Integer.parseInt(m.group(1));
						ep = Integer.parseInt(m.group(2));
						ep_end = Integer.parseInt(m.group(3));

						break;
					case 4:
						season = Integer.parseInt(m.group(1));
						ep = Integer.parseInt(m.group(2));
						season_end = Integer.parseInt(m.group(3));
						ep_end = Integer.parseInt(m.group(4));
					}

				} else {
					if (m.groupCount() == 1) {
						ep = Integer.parseInt(m.group(1));
					} else {
						ep = Integer.parseInt(m.group(1));
						ep_end = Integer.parseInt(m.group(2));
					}
					season = 1;
				}
			}
		} else {
			logMsg("Episode Checking: " + s);
			logMsg("Using Custom Pattern: \"" + customPattern.pattern()
					+ "\" Matching Mode: " + mode.toString());
			Matcher m = customPattern.matcher(s);
			if (m.find()) {
				switch (mode) {
				case Episode:
					season = season_end = 1;
					ep = ep_end = Integer.parseInt(m.group(1));
					break;
				case Episode_Episode:
					season = season_end = 1;
					ep = Integer.parseInt(m.group(1));
					ep_end = Integer.parseInt(m.group(2));
					break;
				case Season_Episode:
					season = season_end = Integer.parseInt(m.group(1));
					ep = ep_end = Integer.parseInt(m.group(2));
					break;
				case Season_Episode_Episode:
					season = season_end = Integer.parseInt(m.group(1));
					ep = Integer.parseInt(m.group(2));
					ep_end = Integer.parseInt(m.group(3));
					break;
				case Season_Episode_Season_Episode:
					season = Integer.parseInt(m.group(1));
					ep = Integer.parseInt(m.group(2));
					season_end = Integer.parseInt(m.group(3));
					ep_end = Integer.parseInt(m.group(4));
					break;
				}
			} else {
				logMsg("Custom Pattern didn't Match.");
			}

		}
		if (lessThan100Episodes && ep > 100) {
			season = ep / 100;
			ep = ep % 100;
		}
		logMsg("Episode: S" + season + "E" + ep + " - S" + season_end + "E"
				+ ep_end);
		if (season_end == -1) {
			season_end = season;
		}
		if (ep_end == -1) {
			ep_end = ep;
		}
	}

	/**
	 * @return the ep
	 */
	public int getEpisode () {
		return ep;
	}

	/**
	 * @return the ep_end
	 */
	public int getEpisode_end () {
		return ep_end;
	}

	/**
	 * @return the season
	 */
	public int getSeason () {
		return season;
	}

	/**
	 * @return the season_end
	 */
	public int getSeason_end () {
		return season_end;
	}

	/**
	 * @return the version
	 */
	public int getVersion () {
		return version;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString () {
		return "SeasonStart: " + season + " SeasonEnd: " + season_end
				+ " EpisodeStart: " + ep + " EpisodeEnd: " + ep_end
				+ " Version: " + version;
	}

	public String toStringShort () {
		return "S"
				+ season
				+ "E"
				+ ep
				+ ((season != season_end) ? " - S" + season_end + "E" + ep_end
						: ((ep != ep_end) ? " - E" + ep_end : ""))
				+ ((version > 1) ? "v" + version : "");

	}

	private void logMsg (String msg) {
		if (actionProvider != null) {
			actionProvider.log(ActionProvider.LOG_DEBUG, msg);
		}
	}
}
