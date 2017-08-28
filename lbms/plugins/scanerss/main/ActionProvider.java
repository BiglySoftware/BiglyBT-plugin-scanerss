package lbms.plugins.scanerss.main;

import java.io.IOException;


/**
 * @author Damokles
 *
 */
public abstract class ActionProvider {

	public static final int LOG_DEBUG	= 1;
	public static final int LOG_INFO	= 2;
	public static final int LOG_WARN	= 3;
	public static final int LOG_ERROR	= 4;
	public static final int LOG_FATAL	= 5;

	private boolean saveRSS = true;

	public String getDefaultUserAgent () {
		return "ScaneRSS";
	}

	public String getPluginDir() {
			return ".";
	}

	public void log (int level, String msg) {
		System.out.println(msg);
	}

	public void log (int level, String msg, Object o) {
		log(level,msg);
	}

	public void log (Throwable e) {
		log(LOG_ERROR, e.getMessage());
	}

	public void log (String msg, Throwable e) {
		log(LOG_ERROR, msg);
	}

	public void log (String msg, Throwable e, Object o) {
		log(LOG_ERROR, msg);
	}

	public void addTorrent (RSSItem item) {}

	public void addTorrent (RSSItem item, ITorrent torrent) {}

	public void scrapeTorrent (RSSItem item) {}

	public ITorrent getTorrent (RSSItem item) throws IOException {
		return null;
	}

	public boolean isAddTorrentSupported () {
		return false;
	}

	public boolean isScrapeTorrentSupported () {
		return false;
	}

	public void disableSave(boolean x) {
		saveRSS = !x;
	}

	/**
	 * Requests a save of the Data.
	 * 
	 * The implementing method should call @see RSSFeedManager.saveTo*
	 */
	public final void saveRSSData() {
		if (saveRSS) saveRSSDataImpl();
	}

	protected abstract void saveRSSDataImpl();

	public abstract String getConfigValue (String key);
	public abstract void setConfigValue   (String key, String value);

	public boolean getConfigValueAsBoolean (String key) {
		return Boolean.parseBoolean(getConfigValue(key));
	}

	/**
	 * Searches for the property with the specified key in this property list. 
	 * If the key is not found in this property list, the default property list,
	 * and its defaults, recursively, are then checked.
	 * The method returns the default value argument if the property is not found.
	 * 
	 * @param key the property key.
	 * @param def a default value.
	 * @return
	 */
	public boolean getConfigValueAsBoolean (String key, boolean def) {
		String prop = getConfigValue(key);
		if (prop == null)
			return def;
		else
			return Boolean.parseBoolean(getConfigValue(key));
	}

	/**
	 * Searches for the property with the specified key in this property list.
	 * If the key is not found in this property list, the default property list,
	 * and its defaults, recursively, are then checked. 
	 * The method returns 0 if the property is not found.
	 *
	 * @param key the property key.
	 * @return
	 */
	public int getConfigValueAsInt (String key) {
		String prop = getConfigValue(key);
		if (prop == null) return 0;
		try {
			return Integer.parseInt(prop);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Searches for the property with the specified key in this property list. 
	 * If the key is not found in this property list, the default property list,
	 * and its defaults, recursively, are then checked.
	 * The method returns the default value argument if the property is not found.
	 * 
	 * @param key the property key.
	 * @param def a default value.
	 * @return
	 */
	public int getConfigValueAsInt (String key, int def) {
		String prop = getConfigValue(key);
		if (prop == null) return def;
		try {
			return Integer.parseInt(prop);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return def;
		}
	}

	/**
	 * Searches for the property with the specified key in this property list.
	 * If the key is not found in this property list, the default property list,
	 * and its defaults, recursively, are then checked. 
	 * The method returns 0 if the property is not found.
	 * 
	 * @param key the property key.
	 * @return
	 */
	public long getConfigValueAsLong (String key) {
		String prop = getConfigValue(key);
		if (prop == null) return 0;
		try {
			return Long.parseLong(prop);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Searches for the property with the specified key in this property list. 
	 * If the key is not found in this property list, the default property list,
	 * and its defaults, recursively, are then checked.
	 * The method returns the default value argument if the property is not found.
	 * 
	 * @param key the property key.
	 * @param def a default value.
	 * @return
	 */
	public long getConfigValueAsLong (String key, long def) {
		String prop = getConfigValue(key);
		if (prop == null) return def;
		try {
			return Long.parseLong(prop);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return def;
		}
	}

	/**
	 * Searches for the property with the specified key in this property list.
	 * If the key is not found in this property list, the default property list,
	 * and its defaults, recursively, are then checked. 
	 * The method returns 0 if the property is not found.
	 *
	 * @param key the property key.
	 * @return
	 */
	public float getConfigValueAsFloat (String key) {
		String prop = getConfigValue(key);
		if (prop == null) return 0;
		try {
			return Float.parseFloat(prop);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Searches for the property with the specified key in this property list. 
	 * If the key is not found in this property list, the default property list,
	 * and its defaults, recursively, are then checked.
	 * The method returns the default value argument if the property is not found.
	 * 
	 * @param key the property key.
	 * @param def a default value.
	 * @return
	 */
	public float getConfigValueAsFloat (String key, float def) {
		String prop = getConfigValue(key);
		if (prop == null) return def;
		try {
			return Float.parseFloat(prop);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return def;
		}
	}

	/**
	 * Searches for the property with the specified key in this property list.
	 * If the key is not found in this property list, the default property list,
	 * and its defaults, recursively, are then checked. 
	 * The method returns 0 if the property is not found.
	 *
	 * @param key the property key.
	 * @return
	 */
	public double getConfigValueAsDouble (String key) {
		String prop = getConfigValue(key);
		if (prop == null) return 0;
		try {
			return Double.parseDouble(prop);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Searches for the property with the specified key in this property list. 
	 * If the key is not found in this property list, the default property list,
	 * and its defaults, recursively, are then checked.
	 * The method returns the default value argument if the property is not found.
	 * 
	 * @param key the property key.
	 * @param def a default value.
	 * @return
	 */
	public double getConfigValueAsDouble (String key, int def) {
		String prop = getConfigValue(key);
		if (prop == null) return def;
		try {
			return Double.parseDouble(prop);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return def;
		}
	}

	/**
	 * @param key the key to be placed into this property list.
	 * @param value the value corresponding to key.
	 */
	public void setConfigValue (String key, boolean value) {
		setConfigValue(key, Boolean.toString(value));
	}

	/**
	 * @param key the key to be placed into this property list.
	 * @param value the value corresponding to key.
	 */
	public void setConfigValue (String key, int value) {
		setConfigValue(key, Integer.toString(value));
	}

	/**
	 * @param key the key to be placed into this property list.
	 * @param value the value corresponding to key.
	 */
	public void setConfigValue (String key, long value) {
		setConfigValue(key, Long.toString(value));
	}

	/**
	 * @param key the key to be placed into this property list.
	 * @param value the value corresponding to key.
	 */
	public void setConfigValue (String key, float value) {
		setConfigValue(key, Float.toString(value));
	}

	/**
	 * @param key the key to be placed into this property list.
	 * @param value the value corresponding to key.
	 */
	public void setConfigValue (String key, Double value) {
		setConfigValue(key, Double.toString(value));
	}
}
