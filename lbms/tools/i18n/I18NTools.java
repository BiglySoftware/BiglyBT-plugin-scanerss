package lbms.tools.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class I18NTools {

	/**
	 * Writes the Map to the file.
	 * 
	 * The Map is written to the file
	 * key=value
	 * 
	 * @param file
	 * @param map
	 * @throws IOException
	 */
	public static void writeToFile (File file,Map<String, String> map) throws IOException {
		FileWriter fw = null;
		try {
			fw = new FileWriter(file);
			SortedSet<String> keys = new TreeSet<String> (map.keySet()); //sort all entries
			for (String key:keys) {
				fw.write(key+"="+map.get(key).replace("\n", "\\n")+"\n");
			}
		} finally {
			if (fw!=null) fw.close();
		}
	}

	/**
	 * Reads a Map from a File
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Map<String,String> readFromFile (File file) throws IOException {
		Map<String, String> map = new TreeMap<String, String>();
		I18NTranslator.load(new BufferedReader(new FileReader(file)), map);
		return map;
	}

	/**
	 * This duplicates a Map.
	 * 
	 * All Keys are duplicated and the values
	 * are initialized with ""
	 * 
	 * @param map
	 * @return
	 */
	public static Map<String,String> duplicate (Map<String,String> map) {
		Map<String, String> target = new TreeMap<String, String>();
		SortedSet<String> keys = new TreeSet<String> (map.keySet()); //sort all entries
		for (String key:keys) {
			target.put(key, "");
		}
		return target;
	}

	/**
	 * Merges two Maps
	 * 
	 * This will add all keys to then target
	 * that src has and target don't
	 * 
	 * @param src
	 * @param target
	 */
	public static void merge (Map<String,String> src, Map<String,String> target) {
		SortedSet<String> keys = new TreeSet<String> (src.keySet());
		for (String key:keys) {
			if (!target.containsKey(key)) {
				target.put(key, "");
			}
		}
	}
}
