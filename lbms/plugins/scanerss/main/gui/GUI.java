package lbms.plugins.scanerss.main.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import lbms.plugins.scanerss.main.ActionProvider;
import lbms.plugins.scanerss.main.Filter;
import lbms.plugins.scanerss.main.FilterListener;
import lbms.plugins.scanerss.main.I18N;
import lbms.plugins.scanerss.main.ITorrent;
import lbms.plugins.scanerss.main.ImageRepository;
import lbms.plugins.scanerss.main.QueuedRSSItem;
import lbms.plugins.scanerss.main.RSSFeed;
import lbms.plugins.scanerss.main.RSSFeedListener;
import lbms.plugins.scanerss.main.RSSFeedManager;
import lbms.plugins.scanerss.main.RSSFeedManagerListener;
import lbms.plugins.scanerss.main.RSSItem;
import lbms.tools.HTTPDownload;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * @author Damokles
 * 
 */
public class GUI {

	public static final int				MIN_UPDATE_INTERVAL	= 15;

	protected ActionProvider			actionProvider;
	private CTabFolder					tabFolder;
	private CTabItem					mainViewItem, filterViewItem,
			feedOptionsItem, helpTab;

	protected final SimpleDateFormat	logDateFormatter	= new SimpleDateFormat(
																	"yyyy-MM-dd HH:mm:ss");
	protected RSSFeedManager			feedManager;
	private RSSFeedListener				rssFeedListener;

	private ExecutorService				threadPool			= Executors
																	.newCachedThreadPool(new ThreadFactory() {

																		public Thread newThread (
																				Runnable r) {
																			Thread t = new Thread(
																					r);
																			t
																					.setDaemon(true);
																			t
																					.setPriority(Thread.MIN_PRIORITY);
																			return t;
																		}
																	});

	private FilterListener				filterListener;

	private Queue<QueuedRSSItem>		downloadQueue		= new DelayQueue<QueuedRSSItem>();

	private Display						display;

	private boolean						imagesInited		= false;

	// ------------------
	protected Filter[]					filterArray;

	private MainView					mainView;
	private FilterView					filterView;
	private FeedView					feedView;
	private HelpView					helpView;

	public GUI() {
		I18N
				.setDefault("lbms/plugins/scanerss/main/resources/internat/default.lang");
		try {
			I18N.reload();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mainView = new MainView(this);
		filterView = new FilterView(this);
		feedView = new FeedView(this);
		helpView = new HelpView(this);

		filterListener = new FilterListener() {
			public void accepted (final RSSItem item) {
				System.out.println("Item found" + item);
				threadPool.execute(new Runnable() {
					/*
					 * (non-Javadoc)
					 * 
					 * @see java.lang.Runnable#run()
					 */
					public void run () {
						mainView.addAcceptedItem(item);
						// if DownloadHistory is used save the data now
						actionProvider.saveRSSData();
						if (actionProvider.isAddTorrentSupported()) {
							try {
								ITorrent torrent = actionProvider
										.getTorrent(item);

								if (item.getParentFilter() == null
										|| item.getParentFilter().apply(item,
												torrent)) {
									actionProvider.addTorrent(item, torrent);
								}

							} catch (IOException e) {
								downloadQueue.offer(new QueuedRSSItem(item,
										MIN_UPDATE_INTERVAL, TimeUnit.MINUTES));
								actionProvider
										.log(
												"Failed to Download Torrent. Retrying later.",
												e);
							}
						}
						if (actionProvider
								.getConfigValueAsBoolean("logDownloads")) {
							File log = new File(actionProvider
									.getConfigValue("logFile"));
							FileWriter fw = null;

							try {
								fw = new FileWriter(log, true);
								fw.append(logDateFormatter.format(new Date())
										+ "\t" + item.getTitle() + "\t"
										+ item.getLink() + "\tFeed: "
										+ item.getParentFeed().getName()
										+ "\tFilter: "
										+ item.getParentFilter().getName()
										+ "\n");
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								if (fw != null) {
									try {
										fw.close();
									} catch (IOException e) {
									}
								}
							}
						}
						mainView.updateFilteredTable();
					}
				});
			};
		};

		rssFeedListener = new RSSFeedListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.RSSFeedListener#updated(lbms.plugins
			 * .scanerss.main.RSSFeed, boolean)
			 */
			public void updated (RSSFeed feed, boolean newItems) {
				int pos = feedManager.getFeeds().indexOf(feed);
				mainView.updateMainFeedItem(pos, feed, newItems);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.RSSFeedListener#error(java.lang.Exception
			 * )
			 */
			public void error (Exception e) {
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.RSSFeedListener#error(java.lang.String
			 * )
			 */
			public void error (String m) {
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.RSSFeedListener#updating(lbms.plugins
			 * .scanerss.main.RSSFeed)
			 */
			public void updating (RSSFeed feed) {
			}
		};

		actionProvider = new ActionProvider() {
			@Override
			public String getConfigValue (String key) {
				return null;
			}

			@Override
			public void setConfigValue (String key, String value) {
			}

			@Override
			protected void saveRSSDataImpl () {
			}
		};
		feedManager = new RSSFeedManager(actionProvider);
		feedManager.addListener(new RSSFeedManagerListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.RSSFeedManagerListener#feedAdded(lbms
			 * .plugins.scanerss.main.RSSFeed)
			 */
			public void feedAdded (RSSFeed f) {
				f.addListener(rssFeedListener);
				updateFeedTreeAndTable(true);
				actionProvider.saveRSSData();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.RSSFeedManagerListener#feedRemoved
			 * (lbms.plugins.scanerss.main.RSSFeed)
			 */
			public void feedRemoved (RSSFeed f) {
				updateFeedTreeAndTable(true);
				actionProvider.saveRSSData();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.RSSFeedManagerListener#filterAdded
			 * (lbms.plugins.scanerss.main.Filter)
			 */
			public void filterAdded (Filter f) {
				System.out.println("Added Listener to Filter: " + f.getName());
				filterArray = feedManager.getFilterAsArray();
				Arrays.sort(filterArray);
				f.addFilterListener(filterListener);
				updateFilterTables();
				actionProvider.saveRSSData();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.RSSFeedManagerListener#filterRemoved
			 * (lbms.plugins.scanerss.main.Filter)
			 */
			public void filterRemoved (Filter f) {
				filterArray = feedManager.getFilterAsArray();
				Arrays.sort(filterArray);
				updateFilterTables();
				actionProvider.saveRSSData();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.RSSFeedManagerListener#updatedComplete
			 * ()
			 */
			public void updatedComplete () {
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.scanerss.main.RSSFeedManagerListener#reset()
			 */
			public void reset () {
				filterArray = new Filter[0];
				resetAll();
			}
		});

		filterArray = feedManager.getFilterAsArray();
	}

	protected void updateFeedTreeAndTable (final boolean clearFirst) {
		mainView.updateFeedTree(clearFirst);
		feedView.updateFeedTable();
	}

	protected void updateFilterTables () {
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(new SWTSafeRunnable() {
			@Override
			public void runSafe () {
				feedView.updateFilterTable();
				filterView.updateFilterTables();
			}
		});
	}

	protected void resetAll () {
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(new SWTSafeRunnable() {
			@Override
			public void runSafe () {
				filterView.reset();
				mainView.reset();
				feedView.reset();
			}
		});
	}

	protected void updateAll () {
		if (display != null && !display.isDisposed()) {
			display.asyncExec(new SWTSafeRunnable() {
				@Override
				public void runSafe () {
					filterArray = feedManager.getFilterAsArray();
					updateFeedTreeAndTable(true);
					updateFilterTables();
				}
			});
		}
	}

	public void retryFailedDownloads () {
		if (actionProvider.isAddTorrentSupported() && !downloadQueue.isEmpty()) {
			threadPool.execute(new Runnable() {
				public void run () {
					QueuedRSSItem queued = downloadQueue.poll();
					while (queued != null) {

						RSSItem item = queued.getRssItem();
						actionProvider.log(ActionProvider.LOG_INFO,
								"Retrying Torrent: " + item.getTitle());
						try {
							ITorrent torrent = actionProvider.getTorrent(item);

							if (item.getParentFilter() == null
									|| item.getParentFilter().apply(item,
											torrent)) {
								actionProvider.addTorrent(item, torrent);
							}
						} catch (IOException e) {
							downloadQueue.offer(new QueuedRSSItem(item,
									MIN_UPDATE_INTERVAL, TimeUnit.MINUTES));
							actionProvider
									.log(
											"Failed to Download Torrent. Retrying later.",
											e);
						}

						queued = downloadQueue.poll();
					}
				}
			});
		}
	}

	public void setActionProvider (ActionProvider ap) {
		actionProvider = ap;
		feedManager.setActionProvider(ap);
	}

	/**
	 * @return the actionProvider
	 */
	public ActionProvider getActionProvider () {
		return actionProvider;
	}

	public void createContents (final Composite parent) {
		display = parent.getDisplay();
		if (!imagesInited) {
			imagesInited = true;
			ImageRepository.loadImages(display);
			if (actionProvider.getConfigValueAsBoolean("useCustomFeedIcons")) {
				List<RSSFeed> feeds = feedManager.getFeeds();
				for (RSSFeed f : feeds) {
					File ico = new File(actionProvider.getPluginDir(),
							"iconcache/" + f.getSourceURL().getHost() + ".ico");
					if (ico.exists()) {
						ImageRepository.loadImage(display, ico, f
								.getSourceURL().getHost()
								+ ".ico");
					}
				}

			}
		}

		tabFolder = new CTabFolder(parent, SWT.BORDER);
		tabFolder.setLayout(new GridLayout(1, false));
		GridData gridData = new GridData(GridData.FILL_BOTH);

		gridData.horizontalSpan = 2;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		tabFolder.setLayoutData(gridData);
		tabFolder.setSimple(false);

		tabFolder.setSelectionForeground(display
				.getSystemColor(SWT.COLOR_TITLE_FOREGROUND));
		tabFolder.setSelectionBackground(new Color[] {
				display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND),
				display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT) },
				new int[] { 100 }, true);

		// ----- First Tab: FeedView ------\\
		mainViewItem = new CTabItem(tabFolder, SWT.NULL);
		mainViewItem.setText("Main View");
		mainView.onAttach(tabFolder, mainViewItem);

		// -----Second Tab: Filter View ----\\

		filterViewItem = new CTabItem(tabFolder, SWT.NULL);
		filterViewItem.setText("Filter View");
		filterView.onAttach(tabFolder, filterViewItem);

		// -----Third Tab: Feed Options ----\\
		feedOptionsItem = new CTabItem(tabFolder, SWT.NULL);
		feedOptionsItem.setText("Feed Options");
		feedView.onAttach(tabFolder, feedOptionsItem);

		// -------Fourth Tab: Help -----\\

		helpTab = new CTabItem(tabFolder, SWT.NULL);
		helpTab.setText("Help");
		helpView.onAttach(tabFolder, helpTab);

		updateFilterTables();
		updateFeedTreeAndTable(true);

		// Need to select the main tab as to force it to draw correctly
		// This is ABSOLUTELY a SWT3.2+ call!!!
		tabFolder.setSelection(mainViewItem);
	}

	/**
	 * @return the mainView
	 */
	protected MainView getMainView () {
		return mainView;
	}

	/**
	 * @return the filterView
	 */
	protected FilterView getFilterView () {
		return filterView;
	}

	/**
	 * @return the feedView
	 */
	protected FeedView getFeedView () {
		return feedView;
	}

	/**
	 * @return the helpView
	 */
	protected HelpView getHelpView () {
		return helpView;
	}

	protected void focusView (ScaneRSSView view) {

	}

	/**
	 * @return the feedManager
	 */
	public RSSFeedManager getFeedManager () {
		return feedManager;
	}

	public void reloadFeedIcons () {
		Thread dlT = new Thread() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Thread#run()
			 */
			@Override
			public void run () {
				List<RSSFeed> feeds = feedManager.getFeeds();
				for (RSSFeed f : feeds) {
					try {
						if (f.getSourceURL().getProtocol().equalsIgnoreCase(
								"file")) {
							continue;
						}
						File ico = new File(actionProvider.getPluginDir(),
								"iconcache/" + f.getSourceURL().getHost()
										+ ".ico");
						HTTPDownload icoDl = new HTTPDownload(new URL(f
								.getSourceURL(), "/favicon.ico"), ico);
						icoDl.call();
						if (!icoDl.hasFailed() && ico.exists()) {
							InputStream is = null;

							try {
								is = new FileInputStream(ico);
								byte[] icoHeader = new byte[6];
								int len = is.read(icoHeader);
								is.close();
								if (len < 6) {
									ico.delete();
									continue;
								} else {
									if (icoHeader[0] == 0
											&& icoHeader[1] == 0
											&& icoHeader[2] == 1
											&& icoHeader[3] == 0
											&& (icoHeader[4] == 1 || icoHeader[4] == 2)
											&& icoHeader[5] == 0) {
										ImageRepository.loadImage(display, ico,
												f.getSourceURL().getHost()
														+ ".ico");
									} else {
										ico.delete();
									}
								}
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								if (is != null) {
									is.close();
								}
							}

						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				updateFeedTreeAndTable(true);
			}
		};
		dlT.setDaemon(true);
		dlT.setPriority(Thread.MIN_PRIORITY);
		dlT.start();
	}

	/**
	 * This Method should be overridden to support AzSMRC Users
	 * 
	 * @return
	 */
	protected String[] getAzSMRCUsernames () {
		return new String[0];
	}

}
