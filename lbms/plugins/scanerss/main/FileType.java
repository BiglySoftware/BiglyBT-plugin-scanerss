/**
 * 
 */
package lbms.plugins.scanerss.main;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jdom.Element;

/**
 * @author Leonard
 * 
 */
public class FileType {

	private Set<String>		patterns	= new HashSet<String>(1);
	private String			name;
	private String			description;
	private int				id;
	private Set<FileType>	subtypes	= null;

	/**
	 * 
	 */
	public FileType(String name, int id) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	public FileType(Element e) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * This Method ..
	 * 
	 * @param str
	 * @return true if str matched
	 */
	public boolean match (String str) {
		str = str.toLowerCase();
		for (String p : patterns) {
			if (str.contains(p)) {
				return true;
			}
		}
		if (subtypes != null) {
			for (FileType f : subtypes) {
				if (f.match(str)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This method is used to check if a new SubType would induce a cycle.
	 * 
	 * @param idCheck
	 * @return true if a cycle would be created
	 */
	private boolean checkCycle (FileType newType) {
		if (newType == this) {
			return true;
		} else if (subtypes != null) {
			for (FileType f : subtypes) {
				if (f.checkCycle(newType)) {
					return true;
				}
			}
		}
		return false;
	}

	public String getName () {
		return name;
	}

	public void setName (String name) {
		this.name = name;
	}

	public String getDescription () {
		return description;
	}

	public void setDescription (String description) {
		this.description = description;
	}

	public Set<String> getPatterns () {
		return Collections.unmodifiableSet(patterns);
	}

	public Set<FileType> getSubtypes () {
		if (subtypes != null) {
			return Collections.unmodifiableSet(subtypes);
		} else {
			return null;
		}
	}

	protected void setSubtypes (Set<FileType> filetypes) {
		subtypes = new HashSet<FileType>(filetypes);
	}

	public void addPattern (String pattern) {
		if (pattern.length() > 0) {
			patterns.add(pattern);
		}
	}

	public boolean addSubType (FileType sub) {
		if (checkCycle(sub)) {
			return false;
		}
		if (subtypes == null) {
			subtypes = new HashSet<FileType>();
		}
		subtypes.add(sub);
		return true;
	}

	public static String getElementName () {
		return "FileType";
	}

	/**
	 * @return the id
	 */
	public int getID () {
		return id;
	}
}
