package lbms.plugins.scanerss.main;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import lbms.tools.CryptoTools;

import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class HashDownloadHistory {

	private Set<String> history = new TreeSet<String>();

	public static String getElementName() {
		return "HashDownloadHistory";
	}

	public HashDownloadHistory() {

	}

	public HashDownloadHistory (Element e) {
		List<Element> children = e.getChildren("Ep");
		for (Element c:children) {
			history.add(c.getTextTrim());
		}
	}

	public boolean check(String str) {
		return !history.contains(encode(str));
	}

	public boolean checkAndAdd(String str) {
		if (history.contains(encode(str))) return false;
		else {
			history.add(encode(str));
			return true;
		}
	}

	private String encode (String str) {
		try {
			return CryptoTools.formatByte(CryptoTools.messageDigest(str.getBytes(), "MD5"), false);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return "";
		}
	}

	public Element toElement() {
		Element root = new Element("HashDownloadHistory");
		for (String h:history) {
			Element ep = new Element ("Ep");
			ep.setText(h);
			root.addContent(ep);
		}
		return root;
	}

}
