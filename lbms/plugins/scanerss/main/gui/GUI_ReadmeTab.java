/*
 * Created on Feb 18, 2006
 * Created by omschaub
 *
 */
package lbms.plugins.scanerss.main.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;

public class GUI_ReadmeTab {

	private String					paypalURLUS		= "https://www.paypal.com/cgi-bin/webscr?cmd=_xclick&business=damokles%40users%2esourceforge%2enet&item_name=ScaneRSS%20Donation&no_shipping=0&no_note=1&tax=0&currency_code=USD&bn=PP%2dDonationsBF&charset=UTF%2d8";
	private String					paypalURLEU		= "https://www.paypal.com/cgi-bin/webscr?cmd=_xclick&business=damokles%40users%2esourceforge%2enet&item_name=ScaneRSS%20Donation&no_shipping=0&no_note=1&tax=0&currency_code=EUR&bn=PP%2dDonationsBF&charset=UTF%2d8";
	private String					amazonComURL	= "http://www.amazon.com/exec/obidos/redirect?tag=azsmrc-20&amp;creative=374005&amp;camp=211041&amp;link_code=qs1&amp;adid=0TY7KZ926FVJDA9X0AQ9&amp;path=subst/home/home.html";
	// private String amazonDeURL = "http://www.amazon.de/exec/obidos/redirect-home?tag=azsmrc-21&site=home";
	private String					homepageURL		= "http://azsmrc.sourceforge.net";
	private String					projectpageURL	= "http://www.sourceforge.net/projects/azsmrc";
	private String					downloadsURL	= "http://sourceforge.net/project/showfiles.php?group_id=163110";
	private String					forumsURL		= "http://sourceforge.net/forum/?group_id=163110";

	private static GUI_ReadmeTab	instance;
	private CTabItem				detailsTab;
	private CTabFolder				parentTab;
	private Composite				composite;
	private Label					htFeedSetup;
	private ExpandItem				htItem1;

	private GUI_ReadmeTab (final Display display, CTabFolder _parentTab) {
		instance = this;
		parentTab = _parentTab;
		detailsTab = new CTabItem(parentTab, SWT.NULL);
		detailsTab.setText("Help");

		detailsTab.addDisposeListener(new DisposeListener() {
			public void widgetDisposed (DisposeEvent arg0) {
				if (instance != null) {
					instance = null;
				}
			}
		});

		final ScrolledComposite sc = new ScrolledComposite(parentTab,
				SWT.V_SCROLL);

		composite = new Composite(sc, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 1;
		composite.setLayoutData(gridData);

		sc.setContent(composite);
		sc.setExpandVertical(true);
		sc.setExpandHorizontal(true);

		sc.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized (ControlEvent e) {
				Rectangle r = sc.getClientArea();
				sc.setMinSize(composite.computeSize(r.width, SWT.DEFAULT));
			}
		});
		detailsTab.setControl(sc);

	}

	public static void open (final Display display, final CTabFolder parentTab) {
		if (display == null) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run () {
				if (instance == null) {
					new GUI_ReadmeTab(display, parentTab);
				} else {
					instance.parentTab.setSelection(instance.detailsTab);
				}
			}

		});
	}

}// EOF
