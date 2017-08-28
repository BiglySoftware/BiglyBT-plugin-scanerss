package lbms.plugins.scanerss.main.gui;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import lbms.plugins.scanerss.main.Filter;
import lbms.plugins.scanerss.main.ImageRepository;
import lbms.plugins.scanerss.main.RSSFeed;
import lbms.plugins.scanerss.main.RSSItem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author Damokles
 * 
 */
public class MainView extends ScaneRSSView {

	private Menu				feedMenu, feedIMenu, feedFilterMenu,
			lookupMenu;
	private MenuItem			mAdd, mScrape, mOpenPatternAssitant,
			lookupMenuItem;
	private MenuItem			mIAdd, mIScrape;
	private MenuItem			mApplyFilters;

	protected ToolBar			toolBar;
	private Tree				feedTree;
	private Table				filteredTable;

	private StyledText			detailText;

	private Color				oldItemColor, newItemColor;

	private SimpleDateFormat	lastUpdateFormat	= new SimpleDateFormat(
															"d/M, HH:mm");
	private List<RSSItem>		acceptedItems		= new Vector<RSSItem>();

	public MainView(GUI parent) {
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
		GridLayout gl = new GridLayout(2, false);
		composite.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 600;
		gd.heightHint = 600;
		composite.setLayoutData(gd);

		// End the first tab and set the control to the mainComp
		tabItem.setControl(composite);

		if (oldItemColor == null || oldItemColor.isDisposed()) {
			oldItemColor = new Color(display, 0, 0, 0);
		}
		if (newItemColor == null || newItemColor.isDisposed()) {
			newItemColor = new Color(display, 255, 0, 0);
		}

		// ----------ToolBar across top of tab-------\\
		toolBar = new ToolBar(composite, SWT.HORIZONTAL | SWT.FLAT);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		toolBar.setLayoutData(gd);

		// Update toolbar button
		ToolItem updateItem = new ToolItem(toolBar, SWT.PUSH);
		updateItem.setToolTipText("Update");
		updateItem.setImage(ImageRepository.getImage("update"));
		updateItem.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected (SelectionEvent e) {
				parent.getFeedManager().updateAllFeeds();
			}
		});

		new ToolItem(toolBar, SWT.SEPARATOR);

		ToolItem reloadIcons = new ToolItem(toolBar, SWT.PUSH);
		reloadIcons.setImage(ImageRepository.getImage("redownload"));
		reloadIcons
				.setToolTipText("Re-download all custom icons for subscribed feeds");
		reloadIcons.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				parent.reloadFeedIcons();
			}
		});

		ToolItem saveRSSData = new ToolItem(toolBar, SWT.PUSH);
		saveRSSData.setImage(ImageRepository.getImage("save"));
		saveRSSData.setToolTipText("Backup your Config");
		saveRSSData.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				FileDialog fd = new FileDialog(composite.getShell(), SWT.SAVE);
				fd.setText("Select where to save the Backup");
				fd.setFileName("rss.xml.gz");
				fd.setFilterPath(parent.getActionProvider().getConfigValue(
						"lastBackupPath"));
				String filename = fd.open();
				if (filename != null) {
					try {
						File f = new File(filename);
						parent.getActionProvider().setConfigValue(
								"lastBackupPath", f.getParent());
						parent.getFeedManager().saveToFile(f);
					} catch (IOException e) {
						MessageBox mb = new MessageBox(composite.getShell(),
								SWT.ICON_ERROR | SWT.OK);
						mb.setMessage("Error couldn't save File.\n"
								+ e.getMessage());
						mb.setText("Error on save.");
						mb.open();
					}
				}
			}
		});

		ToolItem loadRSSData = new ToolItem(toolBar, SWT.PUSH);
		loadRSSData.setImage(ImageRepository.getImage("fileopen"));
		loadRSSData
				.setToolTipText("Load config from Backup, this will override your current Config");
		loadRSSData.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				FileDialog fd = new FileDialog(composite.getShell(), SWT.OPEN);
				fd
						.setText("Select which Backup to load.\nAll Settings will be overridden.");
				fd.setFileName("rss.xml.gz");
				fd.setFilterPath(parent.getActionProvider().getConfigValue(
						"lastBackupPath"));
				String filename = fd.open();
				if (filename != null) {
					try {
						File f = new File(filename);
						parent.getActionProvider().setConfigValue(
								"lastBackupPath", f.getParent());
						parent.getFeedManager().reset();
						parent.getFeedManager().loadFromFile(f);
					} catch (IOException e) {
						MessageBox mb = new MessageBox(composite.getShell(),
								SWT.ICON_ERROR | SWT.OK);
						mb.setMessage("Error couldn't load File.\n"
								+ e.getMessage());
						mb.setText("Error on Load.");
						mb.open();
					}
				}
			}
		});

		// -----------Top/Bottom SashForm for below the tree and table-----\\
		SashForm mainSash = new SashForm(composite, SWT.VERTICAL);
		mainSash.setLayout(new GridLayout());
		GridData gridData = new GridData(GridData.FILL_BOTH);
		mainSash.setLayoutData(gridData);

		// -----------SashForm for tree and table-----\\
		SashForm sash = new SashForm(mainSash, SWT.HORIZONTAL);
		sash.setLayout(new GridLayout());
		gridData = new GridData(GridData.FILL_BOTH);
		sash.setLayoutData(gridData);

		// ----- Main Feed Table
		feedTree = new Tree(sash, SWT.V_SCROLL | SWT.BORDER | SWT.VIRTUAL);
		feedTree.addListener(SWT.SetData, new Listener() { // Tree Item ADD
					// CODE
					public void handleEvent (Event event) {
						TreeItem item = (TreeItem) event.item;
						if (item == null || item.isDisposed()) {
							return;
						}
						TreeItem parentItem = item.getParentItem();
						if (parentItem == null) {
							RSSFeed f = parent.getFeedManager().getFeeds().get(
									feedTree.indexOf(item));
							item
									.setText(f.getName()
											//+ " ("
											//+ f.getSourceURL().toExternalForm()
											+ " ["
											+ (f.getLastUpdate() != 0 ? lastUpdateFormat
													.format(new Date(
															f.getLastUpdate() * 1000))
													: "") + "]");
							item.setData("RSSFeed", f);
							if (ImageRepository.hasImage(f.getSourceURL()
									.getHost()
									+ ".ico")) {
								item.setImage(ImageRepository.getImage(f
										.getSourceURL().getHost()
										+ ".ico"));
							} else {
								item.setImage(ImageRepository
										.getImage("rssIcon"));
							}
							item.setItemCount(f.getItemCount());
						} else {
							if (parentItem.isDisposed()) {
								return;
							}
							RSSFeed f = (RSSFeed) parentItem.getData("RSSFeed");
							RSSItem i = f.getItems().get(
									parentItem.indexOf(item));
							item.setText(i.getTitle());
							item.setData("RSSItem", i);
							if (f.getNewItems().contains(i)) {
								item.setForeground(newItemColor);
							}
						}
					}
				});

		feedTree.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected (SelectionEvent e) {
				TreeItem ti = (TreeItem) e.item;
				if (ti.getParentItem() != null) {
					RSSItem i = (RSSItem) ti.getData("RSSItem");
					setFeedDetailText("Title: " + i.getTitle() + "\nURL: "
							+ i.getLink() + "\nDescription: "
							+ i.getDescription());
				}
			}
		});

		// unfortuantly sometime the texts disappear so we have to check for
		// this
		composite.addListener(SWT.Show, new Listener() {
			public void handleEvent (Event event) {
				TreeItem[] items = feedTree.getItems();
				for (TreeItem i : items) {
					if (i.getText().equals("")) {
						parent.updateFeedTreeAndTable(true);
						return;
					}
				}

			}
		});

		feedTree.setItemCount(parent.getFeedManager().getFeedCount());
		gd = new GridData(GridData.FILL_BOTH);
		feedTree.setLayoutData(gd);

		feedTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown (MouseEvent e) {
				if (e.button == 1) {
					if (feedTree.getItem(new Point(e.x, e.y)) == null) {
						feedTree.deselectAll();

					}

				}
			}
		});

		// Filtered Table
		filteredTable = new Table(sash, SWT.BORDER | SWT.V_SCROLL
				| SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
		gd = new GridData(GridData.FILL_BOTH);
		filteredTable.setLayoutData(gd);
		filteredTable.setHeaderVisible(true);

		TableColumn filteredTCName = new TableColumn(filteredTable, SWT.NONE);
		filteredTCName.setText("Name");
		filteredTCName.setWidth(150);
		TableColumn filteredTCFilter = new TableColumn(filteredTable, SWT.NONE);
		filteredTCFilter.setText("Filter");
		filteredTCFilter.setWidth(120);
		TableColumn filteredTCFeed = new TableColumn(filteredTable, SWT.NONE);
		filteredTCFeed.setText("Feed");
		filteredTCFeed.setWidth(100);
		TableColumn filteredTCTime = new TableColumn(filteredTable, SWT.NONE);
		filteredTCTime.setText("Time");
		filteredTCTime.setWidth(130);

		TableColumn filteredTCEpisode = new TableColumn(filteredTable, SWT.NONE);
		filteredTCEpisode.setText("Episode");
		filteredTCEpisode.setWidth(100);

		filteredTable.addListener(SWT.SetData, new Listener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.
			 * widgets.Event)
			 */
			public void handleEvent (Event event) {
				TableItem item = (TableItem) event.item;
				RSSItem rss = acceptedItems.get(event.index);
				item.setText(0, rss.getTitle());
				item.setText(1, rss.getParentFilter().getName());
				item.setText(2, rss.getParentFeed().getName());
				item.setText(3, parent.logDateFormatter.format(new Date(rss
						.getTimeStamp())));
				item.setText(4, rss.getEpisode().toStringShort());
				item.setData("RSSItem", rss);
			}
		});

		// Menu for table
		feedMenu = new Menu(feedTree);
		final MenuItem mUpdate = new MenuItem(feedMenu, SWT.PUSH);
		mUpdate.setText("Update Feed");
		mUpdate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				TreeItem[] selection = feedTree.getSelection();
				if (selection.length == 0) {
					return;
				}
				for (TreeItem ti : selection) {
					try {
						RSSFeed item = (RSSFeed) ti.getData("RSSFeed");
						if (item == null) {
							return;
						}
						item.readThreaded();
					} catch (ClassCastException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		mApplyFilters = new MenuItem(feedMenu, SWT.CASCADE);
		mApplyFilters.setText("Apply Filter");

		feedFilterMenu = new Menu(mApplyFilters);
		mApplyFilters.setMenu(feedFilterMenu);

		new MenuItem(feedMenu, SWT.SEPARATOR);

		feedIMenu = new Menu(filteredTable);

		if (parent.getActionProvider().isAddTorrentSupported()) {
			mAdd = new MenuItem(feedMenu, SWT.PUSH);
			mAdd.setText("Download");
			mAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected (SelectionEvent e) {
					TreeItem[] selection = feedTree.getSelection();
					if (selection.length == 0) {
						return;
					}
					for (TreeItem ti : selection) {
						try {
							RSSItem item = (RSSItem) ti.getData("RSSItem");
							if (item == null) {
								return;
							}
							parent.getActionProvider().addTorrent(item);
						} catch (ClassCastException e1) {
							e1.printStackTrace();
						}
					}
				};
			});
			mIAdd = new MenuItem(feedIMenu, SWT.PUSH);
			mIAdd.setText("Download");
			mIAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected (SelectionEvent e) {
					TableItem[] selection = filteredTable.getSelection();
					if (selection.length == 0) {
						return;
					}
					for (TableItem ti : selection) {
						try {
							RSSItem item = (RSSItem) ti.getData("RSSItem");
							if (item == null) {
								return;
							}
							parent.getActionProvider().addTorrent(item);
						} catch (ClassCastException e1) {
							e1.printStackTrace();
						}
					}
				};
			});
		}

		if (parent.getActionProvider().isScrapeTorrentSupported()) {
			mScrape = new MenuItem(feedMenu, SWT.PUSH);
			mScrape.setText("Scrape");
			mScrape.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected (SelectionEvent e) {
					TreeItem[] selection = feedTree.getSelection();
					if (selection.length == 0) {
						return;
					}
					for (TreeItem ti : selection) {
						try {
							RSSItem item = (RSSItem) ti.getData("RSSItem");
							if (item == null) {
								return;
							}
							parent.getActionProvider().scrapeTorrent(item);
						} catch (ClassCastException e1) {
							e1.printStackTrace();
						}
					}
				};
			});
			mIScrape = new MenuItem(feedIMenu, SWT.PUSH);
			mIScrape.setText("Scrape");
			mIScrape.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected (SelectionEvent e) {
					TableItem[] selection = filteredTable.getSelection();
					if (selection.length == 0) {
						return;
					}
					for (TableItem ti : selection) {
						try {
							RSSItem item = (RSSItem) ti.getData("RSSItem");
							if (item == null) {
								return;
							}
							parent.getActionProvider().scrapeTorrent(item);
						} catch (ClassCastException e1) {
							e1.printStackTrace();
						}
					}
				};
			});
		}

		mOpenPatternAssitant = new MenuItem(feedMenu, SWT.PUSH);
		mOpenPatternAssitant.setText("Open Pattern Assistant");
		mOpenPatternAssitant.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				TreeItem[] selection = feedTree.getSelection();
				if (selection.length == 0) {
					return;
				}
				for (TreeItem ti : selection) {
					try {
						RSSItem item = (RSSItem) ti.getData("RSSItem");
						if (item == null) {
							return;
						}
						parent.focusView(parent.getFilterView());
						GUIPatternAssistant.open(parent, item, display, parent
								.getFeedManager().getFeedsAsArray());
					} catch (ClassCastException e1) {
						e1.printStackTrace();
					}
				}
			};
		});

		lookupMenuItem = new MenuItem(feedMenu, SWT.CASCADE);
		lookupMenuItem.setText("Lookup");

		lookupMenu = new Menu(lookupMenuItem);
		lookupMenuItem.setMenu(lookupMenu);

		MenuItem lookUpAniDB = new MenuItem(lookupMenu, SWT.PUSH);
		lookUpAniDB.setText("AniDB");
		lookUpAniDB.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				TreeItem[] selection = feedTree.getSelection();
				if (selection.length == 0) {
					return;
				}
				TreeItem ti = selection[0];
				try {
					RSSItem item = (RSSItem) ti.getData("RSSItem");
					if (item == null) {
						return;
					}
					String name = GUIPatternAssistant.suggestName(item
							.getTitle());
					Program
							.launch("http://anidb.info/perl-bin/animedb.pl?show=search&sourceid=scanerss-search&do.search=+Start+Search+&search.anime.name="
									+ name);
				} catch (ClassCastException e1) {
					e1.printStackTrace();
				}
			}
		});

		MenuItem lookUpAnimeNfo = new MenuItem(lookupMenu, SWT.PUSH);
		lookUpAnimeNfo.setText("AnimeNfo");
		lookUpAnimeNfo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				TreeItem[] selection = feedTree.getSelection();
				if (selection.length == 0) {
					return;
				}
				TreeItem ti = selection[0];
				try {
					RSSItem item = (RSSItem) ti.getData("RSSItem");
					if (item == null) {
						return;
					}
					String name = GUIPatternAssistant.suggestName(item
							.getTitle());
					Program
							.launch("http://www.animenfo.com/search.php?action=Go&queryin=anime_titles&option=any&query="
									+ name);
				} catch (ClassCastException e1) {
					e1.printStackTrace();
				}
			}
		});

		MenuItem lookUpANN = new MenuItem(lookupMenu, SWT.PUSH);
		lookUpANN.setText("Anime News Network");
		lookUpANN.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				TreeItem[] selection = feedTree.getSelection();
				if (selection.length == 0) {
					return;
				}
				TreeItem ti = selection[0];
				try {
					RSSItem item = (RSSItem) ti.getData("RSSItem");
					if (item == null) {
						return;
					}
					String name = GUIPatternAssistant.suggestName(item
							.getTitle());
					Program
							.launch("http://www.animenewsnetwork.com/encyclopedia/search.php?searchbox="
									+ name);
				} catch (ClassCastException e1) {
					e1.printStackTrace();
				}
			}
		});

		MenuItem lookUpIMDB = new MenuItem(lookupMenu, SWT.PUSH);
		lookUpIMDB.setText("IMDB");
		lookUpIMDB.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				TreeItem[] selection = feedTree.getSelection();
				if (selection.length == 0) {
					return;
				}
				TreeItem ti = selection[0];
				try {
					RSSItem item = (RSSItem) ti.getData("RSSItem");
					if (item == null) {
						return;
					}
					String name = GUIPatternAssistant.suggestName(item
							.getTitle());
					Program.launch("http://imdb.com/find?s=all&q=" + name);
				} catch (ClassCastException e1) {
					e1.printStackTrace();
				}
			}
		});

		feedTree.setMenu(feedMenu);
		filteredTable.setMenu(feedIMenu);
		composite.layout();

		feedMenu.addMenuListener(new MenuListener() {

			public void menuHidden (MenuEvent arg0) {
				// TODO Auto-generated method stub

			}

			public void menuShown (MenuEvent arg0) {
				TreeItem[] items = feedTree.getSelection();

				for (TreeItem item : items) {
					TreeItem parentTree = item.getParentItem();
					if (parentTree == null) {
						// Parent node
						mUpdate.setEnabled(true);
						mApplyFilters.setEnabled(true);
						if (mAdd != null) {
							mAdd.setEnabled(false);
						}
						if (mScrape != null) {
							mScrape.setEnabled(false);
						}
						lookupMenuItem.setEnabled(false);
						mOpenPatternAssitant.setEnabled(false);
						MenuItem[] mitems = feedFilterMenu.getItems();
						for (MenuItem mi : mitems) {
							mi.dispose();
						}
						RSSFeed feed = (RSSFeed) item.getData("RSSFeed");
						Filter[] filter = parent.getFeedManager().getFilter()
								.values().toArray(new Filter[] {});
						Arrays.sort(filter);
						for (Filter f : filter) {
							if (!f.isActive()) {
								continue; // only active filter work
							}
							MenuItem filterMI = new MenuItem(feedFilterMenu,
									SWT.PUSH);
							filterMI.setText(f.getName());
							filterMI
									.addSelectionListener(new FilterMenuItemSelectionAdapter(
											feed, f));
						}
					} else {
						// Child node
						mUpdate.setEnabled(false);
						mApplyFilters.setEnabled(false);
						if (mAdd != null) {
							mAdd.setEnabled(true);
						}
						if (mScrape != null) {
							mScrape.setEnabled(true);
						}
						lookupMenuItem.setEnabled(true);
						mOpenPatternAssitant.setEnabled(true);
					}

					if (items.length > 1 || items.length == 0) {
						if (mAdd != null) {
							mAdd.setEnabled(false);
						}
						if (mScrape != null) {
							mScrape.setEnabled(false);
						}
						lookupMenuItem.setEnabled(false);
						mUpdate.setEnabled(false);
						mOpenPatternAssitant.setEnabled(false);
					}
				}
			}

		});

		Composite bottomComp = new Composite(mainSash, SWT.NULL);
		gd = new GridData(GridData.FILL_BOTH);
		bottomComp.setLayoutData(gd);
		gl = new GridLayout();
		gl.numColumns = 1;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		bottomComp.setLayout(gl);

		// Styled Text box
		detailText = new StyledText(bottomComp, SWT.WRAP | SWT.BORDER
				| SWT.READ_ONLY | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		detailText.setLayoutData(gd);

		// TODO -- need a way to save this!
		mainSash.setWeights(new int[] { 80, 20 });

		filteredTable.setItemCount(acceptedItems.size());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see lbms.plugins.scanerss.main.gui.ScaneRSSView#onDetach()
	 */
	@Override
	public void onDetach () {

		oldItemColor.dispose();
		newItemColor.dispose();
		oldItemColor = null;
		newItemColor = null;

	}

	/**
	 * @return the toolBar
	 */
	public ToolBar getToolBar () {
		return toolBar;
	}

	protected void updateFilteredTable () {
		if (display == null || display.isDisposed() || feedTree == null
				|| feedTree.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Runnable#run()
			 */
			public void run () {
				filteredTable.clearAll();
				filteredTable.setItemCount(acceptedItems.size());
			}
		});
	}

	private void setFeedDetailText (final String information) {
		display.asyncExec(new SWTSafeRunnable() {
			@Override
			public void runSafe () {
				if (detailText != null && !detailText.isDisposed()) {
					detailText.setText(information);
				}
			}
		});
	}

	public void addAcceptedItem (RSSItem item) {
		acceptedItems.add(item);
		Collections.sort(acceptedItems);
	}

	/**
	 * @return the acceptedItems
	 */
	public List<RSSItem> getAcceptedItems () {
		return acceptedItems;
	}

	protected void reset () {
		if (feedTree != null && !feedTree.isDisposed()) {
			feedTree.removeAll();
		}
	}

	protected void update () {
		if (feedTree != null && !feedTree.isDisposed()) {
			feedTree.removeAll();
		}
	}

	protected void updateMainFeedItem (final int pos, final RSSFeed f,
			final boolean newItems) {
		if (display == null || display.isDisposed() || feedTree == null
				|| feedTree.isDisposed()) {
			return;
		}
		display.asyncExec(new SWTSafeRunnable() {
			@Override
			public void runSafe () {
				TreeItem item = feedTree.getItem(pos);
				item.setText(f.getName()
						// + " ("
						// + f.getSourceURL().toExternalForm()
						+ " ["
						+ lastUpdateFormat.format(new Date(
								f.getLastUpdate() * 1000)) + "]");
				item.clearAll(true);
				item.setItemCount(f.getItemCount());
				if (newItems) {
					item.setForeground(newItemColor);
				} else {
					item.setForeground(oldItemColor);
				}
			}
		});
	}

	protected void updateFeedTree (final boolean clearFirst) {
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(new SWTSafeRunnable() {
			@Override
			public void runSafe () {
				if (feedTree == null || feedTree.isDisposed()) {
					return;
				}

				if (clearFirst) {
					feedTree.clearAll(true);
				}
				feedTree.setItemCount(parent.getFeedManager().getFeedCount());
			}
		});
	}

	protected static class FilterMenuItemSelectionAdapter extends
			SelectionAdapter {
		RSSFeed	rf;
		Filter	f;

		FilterMenuItemSelectionAdapter(RSSFeed rf, Filter f) {
			this.rf = rf;
			this.f = f;
		}

		@Override
		public void widgetSelected (SelectionEvent e) {
			List<RSSItem> items = rf.getItems();
			for (RSSItem item : items) {
				f.apply(item);
			}
		}
	}
}
