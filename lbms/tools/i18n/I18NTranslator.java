package lbms.tools.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class I18NTranslator {

	private Map<String, String>	messages	= new HashMap<String, String>();

	private boolean				initialized, localized;

	public void initialize(File defaultFile) throws IOException {
		messages.clear();
		load(new BufferedReader(new FileReader(defaultFile)), messages);
		initialized = true;
		localized = false;
	}

	public void load(File localizedFile) throws IOException {
		load(new BufferedReader(new FileReader(localizedFile)), messages);
		localized = true;
	}

	public void initialize(InputStream is) throws IOException {
		messages.clear();
		load(new BufferedReader(new InputStreamReader(is)), messages);
		initialized = true;
		localized = false;
	}

	public void load(InputStream is) throws IOException {
		load(new BufferedReader(new InputStreamReader(is)), messages);
		localized = true;
	}

	protected static void load(BufferedReader ir, Map<String, String> map)
			throws IOException {
		try {
			do {
				String line = ir.readLine();
				if (line == null) {
					break; // EOF
				}
				if (line.indexOf("//") == 0 || line.indexOf("#") == 0) {
					continue; // ignore comments
				}
				if (!line.contains("=")) {
					continue; // invalid line
				}
				String[] parts = line.split("=", 2);
				map.put(parts[0], parts[1].replace("\\n", "\n"));
			} while (true);

		} finally {
			if (ir != null) {
				ir.close();
			}
		}
	}

	public String translate(String key) {
		if (messages.containsKey(key)) {
			return messages.get(key);
		} else {
			return key;
		}
	}

	/**
	 * Returns a Localized Message identified by the key.
	 * 
	 * Returns the key if no entry was found. It will replace every instance of
	 * {i} with params[i].toString(), only the length of params is respected so
	 * no ArrayOutOfBoundsException.
	 * 
	 * @param key the key of the message.
	 * @param params replacements for placeholder
	 * @return fully replaced message or key
	 */
	public String translate(String key, Object[] params) {
		if (messages.containsKey(key)) {
			String msg = messages.get(key);
			for (int i = 0; i < params.length; i++) {
				msg = msg.replace("{" + i + "}", params[i].toString());
			}
			return msg;
		} else {
			return key;
		}
	}

	/**
	 * @return Returns the initialized.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * @return Returns the localized.
	 */
	public boolean isLocalized() {
		return localized;
	}
}
