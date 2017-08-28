package lbms.plugins.scanerss.main.gui;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * @author Damokles
 *
 */
public abstract class ScaneRSSView {

	protected Composite	composite;
	protected Display	display;
	protected GUI		parent;
	protected CTabItem	tabItem;

	public ScaneRSSView (GUI parent) {
		super();
		this.parent = parent;

	}

	public void onAttach (CTabFolder parentFolder, CTabItem tabItem) {
		this.tabItem = tabItem;
		this.display = tabItem.getDisplay();
	}

	public abstract void onDetach ();

}
