package lbms.plugins.scanerss.main.gui;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lbms.plugins.scanerss.main.ImageRepository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * @author Damokles
 * 
 */
public class HelpView extends ScaneRSSView {

	private static final String	paypalURLUS		= "https://www.paypal.com/cgi-bin/webscr?cmd=_xclick&business=damokles%40users%2esourceforge%2enet&item_name=ScaneRSS%20Donation&no_shipping=0&no_note=1&tax=0&currency_code=USD&bn=PP%2dDonationsBF&charset=UTF%2d8";
	private static final String	paypalURLEU		= "https://www.paypal.com/cgi-bin/webscr?cmd=_xclick&business=damokles%40users%2esourceforge%2enet&item_name=ScaneRSS%20Donation&no_shipping=0&no_note=1&tax=0&currency_code=EUR&bn=PP%2dDonationsBF&charset=UTF%2d8";
	private static final String	amazonComURL	= "http://www.amazon.com/exec/obidos/redirect?tag=azsmrc-20&amp;creative=374005&amp;camp=211041&amp;link_code=qs1&amp;adid=0TY7KZ926FVJDA9X0AQ9&amp;path=subst/home/home.html";
	// private String amazonDeURL = "http://www.amazon.de/exec/obidos/redirect-home?tag=azsmrc-21&site=home";
	private static final String	homepageURL		= "http://azsmrc.sourceforge.net";
	private static final String	projectpageURL	= "http://www.sourceforge.net/projects/azsmrc";
	private static final String	downloadsURL	= "http://sourceforge.net/project/showfiles.php?group_id=163110";
	private static final String	forumsURL		= "http://sourceforge.net/forum/?group_id=163110";
	private static final String	regexURL		= "http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Pattern.html";

	private Label				htFeedSetup;
	private ExpandItem			htItem1;

	public HelpView(GUI parent) {
		super(parent);
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see lbms.plugins.scanerss.main.gui.ScaneRSSView#onAttach()
	 */
	@Override
	public void onAttach (CTabFolder parentFolder, CTabItem tabItem) {
		super.onAttach(parentFolder, tabItem);
		final ScrolledComposite sc = new ScrolledComposite(parentFolder,
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
		tabItem.setControl(sc);

		final ExpandBar bar = new ExpandBar(composite, SWT.V_SCROLL);
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 1;
		bar.setLayoutData(gridData);

		// donation information on bar
		Composite donateC = new Composite(bar, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginLeft = layout.marginTop = layout.marginRight = layout.marginBottom = 10;
		layout.verticalSpacing = 10;
		donateC.setLayout(layout);
		gridData = new GridData(GridData.GRAB_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 3;
		donateC.setLayoutData(gridData);

		Button donateUS = new Button(donateC, SWT.PUSH);
		donateUS.setImage(ImageRepository.getImage("paypal"));
		donateUS.setToolTipText("Donation via paypal in United States");
		donateUS.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				Program.launch(paypalURLUS);
			}
		});

		Button donateEU = new Button(donateC, SWT.PUSH);
		donateEU.setImage(ImageRepository.getImage("paypal"));
		donateEU.setToolTipText("Donation via paypal in Europe");
		donateEU.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				Program.launch(paypalURLEU);
			}
		});

		Button amazonDonate = new Button(donateC, SWT.PUSH);
		amazonDonate.setImage(ImageRepository.getImage("amazon.com"));
		amazonDonate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				Program.launch(amazonComURL);
			}
		});

		Label paypalUSLabel = new Label(donateC, SWT.NULL);
		paypalUSLabel.setText("US");
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		paypalUSLabel.setLayoutData(gd);

		Label paypalEULabel = new Label(donateC, SWT.NULL);
		paypalEULabel.setText("Europe");
		gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		paypalEULabel.setLayoutData(gd);

		Label amazonLabel = new Label(donateC, SWT.NULL);
		amazonLabel
				.setText("Sponsor us by shopping through our Amazon.com link\nNo donation required on your end as they pay us for you just shopping there");

		ExpandItem item0 = new ExpandItem(bar, SWT.NONE, 0);
		item0.setText("Donation Information");
		item0.setHeight(donateC.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		item0.setControl(donateC);

		item0.setExpanded(true);
		bar.setSpacing(8);

		Composite devs = new Composite(bar, SWT.NULL);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginHeight = 15;
		devs.setLayout(gridLayout);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		devs.setLayoutData(gridData);

		Label leonard = new Label(devs, SWT.NULL);
		leonard.setText("Leonard Br\u00FCnings");
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		gridData.grabExcessHorizontalSpace = true;
		leonard.setLayoutData(gridData);

		Label marc = new Label(devs, SWT.NULL);
		marc.setText("Marc Schaubach");
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		gridData.grabExcessHorizontalSpace = true;
		marc.setLayoutData(gridData);

		ExpandItem item1 = new ExpandItem(bar, SWT.NONE, 1);
		item1.setText("Developers");
		item1.setHeight(devs.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		item1.setControl(devs);
		item1.setExpanded(false);

		Composite inet = new Composite(bar, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginHeight = 10;
		gridLayout.horizontalSpacing = 30;
		inet.setLayout(gridLayout);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		inet.setLayoutData(gridData);

		Label homepage = new Label(inet, SWT.NULL);
		homepage.setText("ScaneRSS Homepage");
		homepage.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
		homepage.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
		homepage.addMouseListener(new MouseListener() {
			public void mouseDoubleClick (MouseEvent arg0) {
			}

			public void mouseDown (MouseEvent arg0) {
			}

			public void mouseUp (MouseEvent arg0) {
				Program.launch(homepageURL);
			}
		});

		Label projectpage = new Label(inet, SWT.NULL);
		projectpage.setText("ScaneRSS Project Page");
		projectpage.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
		projectpage.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
		projectpage.addMouseListener(new MouseListener() {
			public void mouseDoubleClick (MouseEvent arg0) {
			}

			public void mouseDown (MouseEvent arg0) {
			}

			public void mouseUp (MouseEvent arg0) {
				Program.launch(projectpageURL);
			}
		});

		Label downloads = new Label(inet, SWT.NULL);
		downloads.setText("Downloads");
		downloads.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
		downloads.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
		downloads.addMouseListener(new MouseListener() {
			public void mouseDoubleClick (MouseEvent arg0) {
			}

			public void mouseDown (MouseEvent arg0) {
			}

			public void mouseUp (MouseEvent arg0) {
				Program.launch(downloadsURL);
			}
		});

		Label forums = new Label(inet, SWT.NULL);
		forums.setText("ScaneRSS Forums");
		forums.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
		forums.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
		forums.addMouseListener(new MouseListener() {
			public void mouseDoubleClick (MouseEvent arg0) {
			}

			public void mouseDown (MouseEvent arg0) {
			}

			public void mouseUp (MouseEvent arg0) {
				Program.launch(forumsURL);
			}
		});

		ExpandItem item2 = new ExpandItem(bar, SWT.NONE, 2);
		item2.setText("ScaneRSS on the Internet");
		item2.setHeight(inet.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		item2.setControl(inet);
		item2.setExpanded(false);

		InputStream is = null;
		try {
			Map<String, String> map = new HashMap<String, String>();
			is = ImageRepository.class.getClassLoader().getResourceAsStream(
					"lbms/plugins/scanerss/main/resources/HowTo.xml");
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(is);
			Element root = doc.getRootElement();
			List<Element> elems = root.getChildren("chapter");
			for (Element e : elems) {
				map.put(e.getAttributeValue("title"), e.getText());
			}

			// Introduction
			Label htIntro = new Label(bar, SWT.WRAP | SWT.LEFT);
			gridData = new GridData(GridData.FILL_BOTH);
			htIntro.setLayoutData(gridData);
			htIntro.setText(map.get("Introduction"));

			ExpandItem htItem0 = new ExpandItem(bar, SWT.NONE, 3);
			htItem0.setText("Introduction");
			htItem0.setHeight(htIntro.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
			htItem0.setControl(htIntro);
			htItem0.setExpanded(false);

			// Feed setup
			htFeedSetup = new Label(bar, SWT.WRAP);
			gridData = new GridData(GridData.FILL_BOTH);
			htFeedSetup.setLayoutData(gridData);
			htFeedSetup.setText(map.get("Feed Setup"));

			htItem1 = new ExpandItem(bar, SWT.NONE, 4);
			htItem1.setText("Feed Setup");

			htItem1
					.setHeight(htFeedSetup
							.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
			htItem1.setControl(htFeedSetup);
			htItem1.setExpanded(false);

			// Filter Setup
			Label htFilter = new Label(bar, SWT.WRAP | SWT.LEFT);
			gridData = new GridData(GridData.FILL_BOTH);
			htFilter.setLayoutData(gridData);
			htFilter.setText(map.get("Filter Setup"));

			ExpandItem htItem2 = new ExpandItem(bar, SWT.NONE, 5);
			htItem2.setText("Filter Setup");
			htItem2.setHeight(htFilter.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
			htItem2.setControl(htFilter);
			htItem2.setExpanded(false);

			// Manual Control
			Label htManual = new Label(bar, SWT.WRAP | SWT.LEFT);
			gridData = new GridData(GridData.FILL_BOTH);
			htManual.setLayoutData(gridData);
			htManual.setText(map.get("Manual Control"));

			ExpandItem htItem3 = new ExpandItem(bar, SWT.NONE, 6);
			htItem3.setText("Manual Control");
			htItem3.setHeight(htManual.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
			htItem3.setControl(htManual);
			htItem3.setExpanded(false);

			// Important Notes
			Label htNotes = new Label(bar, SWT.WRAP | SWT.LEFT);
			gridData = new GridData(GridData.FILL_BOTH);
			htNotes.setLayoutData(gridData);
			htNotes.setText(map.get("Important Notes"));

			ExpandItem htItem4 = new ExpandItem(bar, SWT.NONE, 6);
			htItem4.setText("Important Notes");
			htItem4.setHeight(htNotes.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
			htItem4.setControl(htNotes);
			htItem4.setExpanded(false);

			// RegEx Summary
			Composite compRE = new Composite(bar, SWT.NULL);
			compRE.setLayout(new GridLayout(1, false));
			gridData = new GridData(GridData.FILL_BOTH);
			compRE.setLayoutData(gridData);

			Label urlLabel = new Label(compRE, SWT.NULL);
			urlLabel
					.setText("For more information on RegEx see the official Java RegEx Page");
			urlLabel
					.setToolTipText("http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Pattern.html");
			urlLabel.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
			urlLabel.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
			urlLabel.addMouseListener(new MouseListener() {
				public void mouseDoubleClick (MouseEvent arg0) {
				}

				public void mouseDown (MouseEvent arg0) {
				}

				public void mouseUp (MouseEvent arg0) {
					Program.launch(regexURL);
				}
			});

			Label htRegEx = new Label(compRE, SWT.WRAP | SWT.LEFT);
			gridData = new GridData(GridData.FILL_BOTH);
			htRegEx.setLayoutData(gridData);
			htRegEx.setText(map.get("RegEx Information"));

			ExpandItem htItem5 = new ExpandItem(bar, SWT.NONE, 7);
			htItem5.setText("RegEx Information");
			htItem5.setHeight(compRE.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
			htItem5.setControl(compRE);
			htItem5.setExpanded(false);

		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (JDOMException e1) {
			e1.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e1) {
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see lbms.plugins.scanerss.main.gui.ScaneRSSView#onDetach()
	 */
	@Override
	public void onDetach () {
		// TODO Auto-generated method stub

	}
}
