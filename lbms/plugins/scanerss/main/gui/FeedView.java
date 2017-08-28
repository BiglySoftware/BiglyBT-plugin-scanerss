package lbms.plugins.scanerss.main.gui;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lbms.plugins.scanerss.main.Filter;
import lbms.plugins.scanerss.main.ImageRepository;
import lbms.plugins.scanerss.main.RSSFeed;
import lbms.tools.Download;
import lbms.tools.DownloadListener;
import lbms.tools.HTTPDownload;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * @author Damokles
 *
 */
public class FeedView extends ScaneRSSView {

	private Table	foTableFeeds;
	private Table	foTableFilterFeeds;
	private Text	foName, foURL, foUpdateInterval;
	private RSSFeed	foSelectedFeed;
	private Text	username, password, referer, userAgent, cookie;
	private Text	feedModLink, feedModLinkReplace;
	private ToolItem	createFeed, saveFeed;

	public FeedView (GUI parent) {
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

		composite = new Composite(parentFolder, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		composite.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);
		// End the third tab and set the control to the comp
		tabItem.setControl(composite);

		// -----------SashForms --------\\
		final SashForm verticalSash = new SashForm(composite, SWT.VERTICAL);
		verticalSash.setLayout(new GridLayout());
		GridData gridData = new GridData(GridData.FILL_BOTH);
		verticalSash.setLayoutData(gridData);

		Composite rlComp = new Composite(verticalSash, SWT.NULL);
		gl = new GridLayout();
		gl.numColumns = 1;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		rlComp.setLayout(gl);
		gridData = new GridData(GridData.FILL_BOTH);
		rlComp.setLayoutData(gridData);

		// bottom checkbox table in the sash
		foTableFilterFeeds = new Table(verticalSash, SWT.BORDER | SWT.CHECK
				| SWT.FULL_SELECTION | SWT.SINGLE | SWT.VIRTUAL);
		gridData = new GridData(GridData.FILL_BOTH);
		foTableFilterFeeds.setLayoutData(gridData);
		foTableFilterFeeds.setHeaderVisible(false);

		foTableFilterFeeds.addListener(SWT.SetData, new Listener() {

			public void handleEvent (Event event) {

				TableItem item = (TableItem) event.item;

				System.out.println("FilterItem Requested");
				if (foSelectedFeed != null) {
					Filter f = parent.filterArray[event.index];
					item.setData("filter", f);
					System.out.println("Feed isSet");
					item.setText(f.getName());
					if (parent.getFeedManager().getLinks().containsKey(
							foSelectedFeed.getId())) {
						Set<Filter> x = parent.getFeedManager().getLinks().get(
								foSelectedFeed.getId());
						if (x.contains(f)) {
							item.setChecked(true);
						} else {
							item.setChecked(false);
						}

					} else {
						item.setChecked(false);
					}
				}
			}
		});

		foTableFilterFeeds.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				if (event.detail == SWT.CHECK || foSelectedFeed != null) {
					TableItem item = (TableItem) event.item;
					Filter f = (Filter) item.getData("filter");
					if (item.getChecked()) {
						parent.getFeedManager().assignFilterToFeed(f,
								foSelectedFeed);
					} else {
						parent.getFeedManager().removeFilterFromFeed(f,
								foSelectedFeed);
					}
					parent.getActionProvider().saveRSSData();
				}

			}
		});

		// Sub Sashform for right/left
		final SashForm rlSash = new SashForm(rlComp, SWT.HORIZONTAL);
		rlSash.setLayout(new GridLayout());
		gridData = new GridData(GridData.FILL_BOTH);
		rlSash.setLayoutData(gridData);

		// table for rlsash
		foTableFeeds = new Table(rlSash, SWT.CHECK | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.SINGLE | SWT.VIRTUAL);
		gridData = new GridData(GridData.FILL_BOTH);
		foTableFeeds.setLayoutData(gridData);
		foTableFeeds.setHeaderVisible(false);

		foTableFeeds.addListener(SWT.SetData, new Listener() {

			public void handleEvent (Event event) {
				TableItem item = (TableItem) event.item;
				RSSFeed feed = parent.getFeedManager().getFeeds().get(
						event.index);
				item.setData("feed", feed);
				item.setText(feed.getName());
				item.setChecked(feed.isActive());
			}
		});

		foTableFeeds.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				if (event.detail == SWT.CHECK || foSelectedFeed != null) {
					TableItem item = (TableItem) event.item;
					RSSFeed rssFeed = (RSSFeed) item.getData("feed");
					rssFeed.setActive(item.getChecked());
					parent.getActionProvider().saveRSSData();
				}
			}
		});

		foTableFeeds.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown (MouseEvent e) {
				if (e.button == 1) {
					if (foTableFeeds.getItem(new Point(e.x, e.y)) == null) {
						foTableFeeds.deselectAll();
						clearFeedControls();
					}

				}
			}
		});

		// Composite for Right side of rlSash
		final ScrolledComposite sc = new ScrolledComposite(rlSash, SWT.V_SCROLL
				| SWT.BORDER);
		gridData = new GridData(GridData.FILL_BOTH);
		sc.setLayoutData(gridData);

		final Composite rParent = new Composite(sc, SWT.BORDER);
		rParent.setLayout(new GridLayout(2, false));
		gridData = new GridData(GridData.FILL_BOTH);
		rParent.setLayoutData(gridData);

		createToolbar(rParent);
		createNormalOptions(rParent);
		createAdvancedOptions(rParent);

		sc.setContent(rParent);
		sc.setExpandVertical(true);
		sc.setExpandHorizontal(true);
		sc.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized (ControlEvent e) {
				sc.setMinSize(rParent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		// Set the weights of the sash
		// TODO ... when you get preferences in here.. set it to the pref or
		// this default
		verticalSash.setWeights(new int[] { 750, 250 });
		rlSash.setWeights(new int[] { 250, 750 });
		// Listener for changing the sash weights
		foTableFeeds.addControlListener(new ControlListener() {
			public void controlMoved (ControlEvent arg0) {

			}

			public void controlResized (ControlEvent arg0) {
				// TODO save the weights to the prefs here.. this is when the
				// user changes the verticalsash weight
				System.out.println("foVerticalSash: "
						+ verticalSash.getWeights()[0] + " | "
						+ verticalSash.getWeights()[1]);
				System.out.println("rlSash: " + rlSash.getWeights()[0] + " | "
						+ rlSash.getWeights()[1]);
			}

		});

		foTableFeeds.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 *
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected (SelectionEvent e) {
				TableItem item = (TableItem) e.item;
				if (item.getData("feed") != null) {
					foSelectedFeed = (RSSFeed) item.getData("feed");
					foName.setText(foSelectedFeed.getName());
					foURL.setText(foSelectedFeed.getSourceURL().toExternalForm());
					foUpdateInterval.setText(Integer.toString(foSelectedFeed.getUpdateInterval() / 60));

					if (foSelectedFeed.getUsername() != null) {
						username.setText(foSelectedFeed.getUsername());
					} else {
						username.setText("");
					}

					if (foSelectedFeed.getPassword() != null) {
						password.setText(foSelectedFeed.getPassword());
					} else {
						password.setText("");
					}

					if (foSelectedFeed.getUserAgent() != null) {
						userAgent.setText(foSelectedFeed.getUserAgent());
					} else {
						userAgent.setText("");
					}

					if (foSelectedFeed.getReferer() != null) {
						referer.setText(foSelectedFeed.getReferer());
					} else {
						referer.setText("");
					}

					if (foSelectedFeed.getCookie() != null) {
						cookie.setText(foSelectedFeed.getCookie());
					} else {
						cookie.setText("");
					}

					if (foSelectedFeed.getModLinkRegex() != null) {
						feedModLink.setText(foSelectedFeed.getModLinkRegex().pattern());
					} else {
						feedModLink.setText("");
					}

					if (foSelectedFeed.getModLinkReplace() != null) {
						feedModLinkReplace.setText(foSelectedFeed.getModLinkReplace());
					} else {
						feedModLinkReplace.setText("");
					}

					parent.updateFilterTables();
				}
			}
		});

		foTableFeeds.deselectAll();

	}

	private void createToolbar (Composite rParent) {

		final ToolBar tb = new ToolBar(rParent, SWT.FLAT | SWT.HORIZONTAL);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		tb.setLayoutData(gridData);

		ToolItem newFeed = new ToolItem(tb, SWT.PUSH);
		newFeed.setImage(ImageRepository.getImage("new"));
		newFeed.setToolTipText("Clear all entries and allow for creation of new feed");
		newFeed.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				clearFeedControls();
			}
		});

		createFeed = new ToolItem(tb, SWT.PUSH);
		createFeed.setImage(ImageRepository.getImage("create"));
		createFeed.setToolTipText("Create a new feed using provided information");
		createFeed.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (foSelectedFeed == null) {
					if (!foURL.getText().equals("")
							&& !foName.getText().equals("")) {
						try {
							final URL feedURL = new URL(foURL.getText());
							final RSSFeed feed = new RSSFeed(foName.getText(),
									feedURL);
							int uInterval = Integer.parseInt(foUpdateInterval.getText()) * 60;
							if (uInterval < 900) {
								uInterval = 900;
								MessageBox mb = new MessageBox(tb.getShell(),
										SWT.ICON_INFORMATION | SWT.OK);
								mb.setText("Interval too low");
								mb.setMessage("The given update interval was below the minimum of every 15 minutes. "
										+ "The value was automatically changed to the minimum allowed.");
								mb.open();
								foUpdateInterval.setText(String.valueOf(uInterval / 60));
							}

							if (username.getText().equals("")) {
								feed.setUsername(null);
							} else {
								feed.setUsername(username.getText());
							}

							if (password.getText().equals("")) {
								feed.setPassword(null);
							} else {
								feed.setPassword(password.getText());
							}

							if (userAgent.getText().equals("")) {
								feed.setUserAgent(null);
							} else {
								feed.setUserAgent(userAgent.getText());
							}

							if (referer.getText().equals("")) {
								feed.setReferer(null);
							} else {
								feed.setReferer(referer.getText());
							}

							if (cookie.getText().equals("")) {
								feed.setCookie(null);
							} else {
								feed.setCookie(cookie.getText());
							}

							if (feedModLink.getText().equals("")
									|| feedModLinkReplace.getText().equals("")) {
								feed.setModLinkRegex(null);
								feed.setModLinkReplace(null);
							} else {
								feed.setModLinkRegex(feedModLink.getText());
								feed.setModLinkReplace(feedModLinkReplace.getText());
							}

							feed.setUpdateInterval(uInterval);
							parent.getFeedManager().addFeed(feed);
							foSelectedFeed = feed;
							if (parent.getActionProvider().getConfigValueAsBoolean(
									"useCustomFeedIcons")
									&& !feedURL.getProtocol().equalsIgnoreCase(
											"file")) {
								HTTPDownload icoDl = new HTTPDownload(
										new URL(feedURL, "/favicon.ico"),
										new File(
												parent.getActionProvider().getPluginDir(),
												"iconcache/"
														+ feedURL.getHost()
														+ ".ico"));
								icoDl.addDownloadListener(new DownloadListener() {
									public void debugMsg (String msg) {
									}

									public void progress (long bytesRead,
											long bytesTotal) {
									}

									public void stateChanged (int oldState,
											int newState) {
										if (newState == Download.STATE_FINISHED) {
											ImageRepository.loadImageThreadSync(
													display,
													new File(
															parent.getActionProvider().getPluginDir(),
															"iconcache/"
																	+ feedURL.getHost()
																	+ ".ico"),
													feedURL.getHost() + ".ico");
											parent.updateFeedTreeAndTable(true);
										}
									}
								});
								Thread dlT = new Thread(icoDl);
								dlT.setDaemon(true);
								dlT.setPriority(Thread.MIN_PRIORITY);
								dlT.start();
							}
						} catch (MalformedURLException e) {
							MessageBox mb = new MessageBox(tb.getShell(),
									SWT.ICON_ERROR | SWT.OK);
							mb.setText("Invalid URL");
							mb.setMessage("The given URL is not valid, please check the URL and try again.");
							mb.open();
							e.printStackTrace();
						}
					}
				}
			}
		});

		saveFeed = new ToolItem(tb, SWT.PUSH);
		saveFeed.setImage(ImageRepository.getImage("save"));
		saveFeed.setToolTipText("Save changes to Feed");
		saveFeed.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (foSelectedFeed != null) {
					if (!foURL.getText().equals("")
							&& !foName.getText().equals("")) {
						foSelectedFeed.setName(foName.getText());
						int uInterval = Integer.parseInt(foUpdateInterval.getText()) * 60;
						if (uInterval < 900) {
							uInterval = 900;
							MessageBox mb = new MessageBox(tb.getShell(),
									SWT.ICON_INFORMATION | SWT.OK);
							mb.setText("Interval too low");
							mb.setMessage("The given update interval was below the minimum of every 15 minutes. "
									+ "The value was automatically changed to the minimum allowed.");
							mb.open();
							foUpdateInterval.setText(String.valueOf(uInterval / 60));
						}
						foSelectedFeed.setUpdateInterval(uInterval);
						try {
							foSelectedFeed.setSourceURL(new URL(foURL.getText()));
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}

						if (username.getText().equals("")) {
							foSelectedFeed.setUsername(null);
						} else {
							foSelectedFeed.setUsername(username.getText());
						}

						if (password.getText().equals("")) {
							foSelectedFeed.setPassword(null);
						} else {
							foSelectedFeed.setPassword(password.getText());
						}

						if (userAgent.getText().equals("")) {
							foSelectedFeed.setUserAgent(null);
						} else {
							foSelectedFeed.setUserAgent(userAgent.getText());
						}

						if (referer.getText().equals("")) {
							foSelectedFeed.setReferer(null);
						} else {
							foSelectedFeed.setReferer(referer.getText());
						}

						if (cookie.getText().equals("")) {
							foSelectedFeed.setCookie(null);
						} else {
							foSelectedFeed.setCookie(cookie.getText());
						}

						if (feedModLink.getText().equals("")
								|| feedModLinkReplace.getText().equals("")) {
							foSelectedFeed.setModLinkRegex(null);
							foSelectedFeed.setModLinkReplace(null);
						} else {
							foSelectedFeed.setModLinkRegex(feedModLink.getText());
							foSelectedFeed.setModLinkReplace(feedModLinkReplace.getText());
						}

						parent.updateFeedTreeAndTable(true);
					} else {

					}
				}
			}
		});

		ToolItem cancelFeed = new ToolItem(tb, SWT.PUSH);
		cancelFeed.setImage(ImageRepository.getImage("cancel"));
		cancelFeed.setToolTipText("Cancel all changes and leave feed unmodified");
		cancelFeed.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				clearFeedControls();
			}
		});

		ToolItem deleteFeed = new ToolItem(tb, SWT.PUSH);
		deleteFeed.setImage(ImageRepository.getImage("delete"));
		deleteFeed.setToolTipText("Delete selected Feed");
		deleteFeed.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (foSelectedFeed != null) {
					MessageBox mb = new MessageBox(tb.getShell(),
							SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					mb.setText("Delete Confirmation");
					mb.setMessage("Are you sure you wish to delete the feed: "
							+ foSelectedFeed.getName());
					int answer = mb.open();
					if (answer == SWT.NO) {
						return;
					}

					parent.getFeedManager().removeFeed(foSelectedFeed);
					parent.updateFeedTreeAndTable(true);
					clearFeedControls();
				}
			}
		});

		new ToolItem(tb, SWT.SEPARATOR);

		ToolItem addAllFeed = new ToolItem(tb, SWT.PUSH);
		addAllFeed.setImage(ImageRepository.getImage("dbAdd"));
		addAllFeed.setToolTipText("Add all Filters to Feed");
		addAllFeed.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				if (foSelectedFeed != null) {
					parent.getFeedManager().addAllFiltersToFeed(foSelectedFeed);
				}
				parent.updateFilterTables();
			}
		});

		ToolItem removeAllFeed = new ToolItem(tb, SWT.PUSH);
		removeAllFeed.setImage(ImageRepository.getImage("dbRemove"));
		removeAllFeed.setToolTipText("Remove all Filters from Feed");
		removeAllFeed.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				if (foSelectedFeed != null) {
					parent.getFeedManager().removeAllFiltersFromFeed(
							foSelectedFeed);
				}
				parent.updateFilterTables();
			}
		});

	}

	private void createNormalOptions (Composite rParent) {
		Group labelComp = new Group(rParent, SWT.NULL);
		labelComp.setText("Feed Data");
		GridLayout gl = new GridLayout();
		gl.numColumns = 2;
		labelComp.setLayout(gl);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		labelComp.setLayoutData(gridData);

		Label foNameLabel = new Label(labelComp, SWT.NULL);
		foNameLabel.setText("Name:");

		foName = new Text(labelComp, SWT.BORDER);
		foName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label foURLLabel = new Label(labelComp, SWT.NULL);
		foURLLabel.setText("URL:");

		foURL = new Text(labelComp, SWT.BORDER);
		foURL.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite updateComp = new Composite(labelComp, SWT.NULL);
		gl = new GridLayout();
		gl.numColumns = 2;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		updateComp.setLayout(gl);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		updateComp.setLayoutData(gridData);

		Label updateLabel = new Label(updateComp, SWT.NULL);
		updateLabel.setText("Update Interval (in Minutes):");

		foUpdateInterval = new Text(updateComp, SWT.BORDER);
		foUpdateInterval.setText(Integer.toString(GUI.MIN_UPDATE_INTERVAL));

		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;
		foUpdateInterval.setLayoutData(gridData);

		foUpdateInterval.addListener(SWT.Verify, new Listener() {
			public void handleEvent (Event e) {
				String string = e.text;
				char[] chars = new char[string.length()];
				string.getChars(0, chars.length, chars, 0);
				for (int i = 0; i < chars.length; i++) {
					if (!('0' <= chars[i] && chars[i] <= '9')) {
						e.doit = false;
						return;
					}
				}
			}
		});

	}

	private void createAdvancedOptions (Composite rParent) {

		Group advGroup = new Group(rParent, SWT.NULL);
		advGroup.setText("Advanced Options");
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 2;
		advGroup.setLayoutData(gridData);
		advGroup.setLayout(new GridLayout(2, false));

		Label usernameLabel = new Label(advGroup, SWT.NULL);
		usernameLabel.setText("Username:");

		username = new Text(advGroup, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		username.setLayoutData(gridData);

		Label passwdLabel = new Label(advGroup, SWT.NULL);
		passwdLabel.setText("Password:");

		password = new Text(advGroup, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		password.setLayoutData(gridData);

		Label refererLabel = new Label(advGroup, SWT.NULL);
		refererLabel.setText("Referer:");

		referer = new Text(advGroup, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		referer.setLayoutData(gridData);

		Label userAgentLabel = new Label(advGroup, SWT.NULL);
		userAgentLabel.setText("User Agent:");

		userAgent = new Text(advGroup, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		userAgent.setLayoutData(gridData);

		Label cookieLabel = new Label(advGroup, SWT.NULL);
		cookieLabel.setText("Cookie:");

		cookie = new Text(advGroup, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cookie.setLayoutData(gridData);

		// verify listener
		Listener verifyListener = new Listener() {
			public void handleEvent (Event e) {
				final Text text = (Text) e.widget;
				try {
					Pattern.compile(text.getText(), Pattern.CASE_INSENSITIVE);

					// Pattern passes
					text.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
					createFeed.setEnabled(true);
					saveFeed.setEnabled(true);

				} catch (PatternSyntaxException pse) {
					// Pattern fails!
					text.setForeground(display.getSystemColor(SWT.COLOR_DARK_RED));
					createFeed.setEnabled(false);
					saveFeed.setEnabled(false);

				}
			}
		};

		Label replaceLabel = new Label(advGroup, SWT.NULL);
		replaceLabel.setText("Link Modification:  Allows you to replace parts of a link to obtain a direct download");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		replaceLabel.setLayoutData(gridData);

		Label filterModLinkLabel = new Label(advGroup, SWT.NULL);
		filterModLinkLabel.setText("Replace This:");

		feedModLink = new Text(advGroup, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		feedModLink.setLayoutData(gridData);
		feedModLink.addListener(SWT.Modify, verifyListener);

		Label filterModLinkReplaceLabel = new Label(advGroup, SWT.NULL);
		filterModLinkReplaceLabel.setText("With This:");

		feedModLinkReplace = new Text(advGroup, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		feedModLinkReplace.setLayoutData(gridData);
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

	protected void reset () {
		if (foTableFeeds != null && !foTableFeeds.isDisposed()
				&& foTableFilterFeeds != null
				&& !foTableFilterFeeds.isDisposed()) {
			foTableFilterFeeds.removeAll();
			foSelectedFeed = null;
			foTableFeeds.removeAll();
		}
	}

	protected void update () {
		if (foTableFilterFeeds != null && !foTableFilterFeeds.isDisposed()
				&& foTableFeeds != null && !foTableFeeds.isDisposed()) {
			foTableFilterFeeds.removeAll();
			foSelectedFeed = null;
			foTableFeeds.removeAll();
		}
	}

	protected void updateFilterTable () {

		foTableFilterFeeds.clearAll();
		if (foSelectedFeed == null) {
			foTableFilterFeeds.setItemCount(0);
		} else {
			foTableFilterFeeds.setItemCount(parent.filterArray.length);
		}
	}

	protected void updateFeedTable () {
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(new SWTSafeRunnable() {
			@Override
			public void runSafe () {
				if (foTableFeeds == null || foTableFeeds.isDisposed()) {
					return;
				}
				foTableFeeds.clearAll();
				foTableFeeds.setItemCount(parent.getFeedManager().getFeedCount());
			}
		});
	}

	protected void clearFeedControls () {
		foSelectedFeed = null;
		foURL.setText("");
		foName.setText("");
		foUpdateInterval.setText(Integer.toString(GUI.MIN_UPDATE_INTERVAL));
		foTableFeeds.deselectAll();
		foTableFilterFeeds.deselectAll();
		foTableFilterFeeds.clearAll();
		foTableFilterFeeds.setItemCount(0);
		username.setText("");
		password.setText("");
		referer.setText("");
		userAgent.setText("");
		cookie.setText("");
		feedModLink.setText("");
		feedModLinkReplace.setText("");
	}

}
