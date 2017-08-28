package lbms.plugins.scanerss.main.gui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lbms.plugins.scanerss.main.ActionProvider;
import lbms.plugins.scanerss.main.DownloadHistory;
import lbms.plugins.scanerss.main.Episode;
import lbms.plugins.scanerss.main.Filter;
import lbms.plugins.scanerss.main.I18N;
import lbms.plugins.scanerss.main.ImageRepository;
import lbms.plugins.scanerss.main.DownloadHistory.EpisodePatternMatching;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * @author Damokles
 * 
 */
public class FilterView extends ScaneRSSView {

	private static final String		FILTER_PFX			= "filter";

	private Table					filterTable;
	private Text					filterName, filterPattern,
			filterDenyPattern;
	private Text					filterMinSeason, filterMaxSeason,
			filterMinEp, filterMaxEp;
	private Text					filterMinSize, filterMaxSize;
	private Combo					filterMinUnitSize, filterMaxUnitSize;
	private Label					filterMinSeasonL, filterMaxSeasonL,
			filterMinEpL, filterMaxEpL;
	private Button					filterUseDownloadHistoryNone,
			filterUseDownloadHistory, filterUseDownloadHistoryHash, dlHistory;
	private ToolItem				deleteFilter, createFilter, newFilter,
			saveFilter, cancelFilter, addAllFilter, removeAllFilter,
			editFeedAssociation;
	private Filter					filterSelected;
	private Button					bRunOnce, filterMoveAfterCompletion;
	private Text					category, saveDir, renameTo;
	private Combo					azsmrcUser;
	private Label					filterDHMode, filterDHPattern;
	private Button					filterDHInputDialog;
	private EpisodePatternMatching	filterCustomDHMode	= EpisodePatternMatching.Normal;
	private Button					dhHistoryUseHigherVersion;
	private Combo					initialStateCombo;
	private Group					advGroup;

	public FilterView(GUI parent) {
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
		GridData gridData = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gridData);

		// End the second tab and set the control to the comp
		tabItem.setControl(composite);

		// -----------SashForms --------\\
		final SashForm sash = new SashForm(composite, SWT.HORIZONTAL);
		sash.setLayout(new GridLayout());
		gridData = new GridData(GridData.FILL_BOTH);
		sash.setLayoutData(gridData);

		filterTable = new Table(sash, SWT.CHECK | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.SINGLE | SWT.VIRTUAL);
		gridData = new GridData(GridData.FILL_BOTH);
		filterTable.setLayoutData(gridData);
		filterTable.setHeaderVisible(false);

		filterTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent (Event event) {
				TableItem item = (TableItem) event.item;
				int index = event.index;	// seen index out of bounds here
				if ( index < parent.filterArray.length ){
					item.setText(parent.filterArray[index].getName());
					item.setData("filter", parent.filterArray[index]);
					item.setChecked(parent.filterArray[index].isActive());
				}
			}
		});

		filterTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				if (event.detail == SWT.CHECK) {
					TableItem item = (TableItem) event.item;
					Filter filter = (Filter) item.getData("filter");
					filter.setActive(item.getChecked());
					parent.getActionProvider().saveRSSData();
				}
			}
		});

		// Composite for Right side of rlSash
		final ScrolledComposite sc = new ScrolledComposite(sash, SWT.V_SCROLL
				| SWT.BORDER);
		gridData = new GridData(GridData.FILL_BOTH);
		sc.setLayoutData(gridData);

		// right Composite parent
		final Composite rComp = new Composite(sc, SWT.BORDER);
		gl = new GridLayout();
		gl.numColumns = 2;
		gl.verticalSpacing = 20;
		rComp.setLayout(gl);
		gridData = new GridData(GridData.FILL_BOTH);
		rComp.setLayoutData(gridData);

		// Toolbar for top of rComp
		createToolbar(rComp);

		createFilterGroup(rComp);

		// -----Download History Group ----\\
		createDownloadHistoryGroup(rComp);
		// -----Advanced Options Group ----\\
		createAdvancedOptions(rComp);

		// Finish up by setting the ScrolledComp
		sc.setContent(rComp);
		sc.setExpandVertical(true);
		sc.setExpandHorizontal(true);
		sc.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized (ControlEvent e) {
				sc.setMinSize(rComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		filterTable.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected (SelectionEvent e) {
				TableItem item = (TableItem) e.item;
				Filter f = (Filter) item.getData("filter");
				if (f == null) {
					parent.getActionProvider().log(ActionProvider.LOG_ERROR,
							"No Filter set for filtertable item: " + item);
					return;
				}
				selectFilter(f);
			}
		});

		filterTable.deselectAll();

		filterTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown (MouseEvent e) {
				if (e.button == 1) {
					if (filterTable.getItem(new Point(e.x, e.y)) == null) {
						filterTable.deselectAll();
						clearFilterControls();
						saveFilter.setEnabled(false);
						deleteFilter.setEnabled(false);
						createFilter.setEnabled(false);
						addAllFilter.setEnabled(false);
						removeAllFilter.setEnabled(false);
						editFeedAssociation.setEnabled(false);
					}

				}
			}
		});

	}

	private void createToolbar (Composite parentComp) {

		final ToolBar tb = new ToolBar(parentComp, SWT.HORIZONTAL | SWT.FLAT);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		tb.setLayoutData(gridData);

		newFilter = new ToolItem(tb, SWT.PUSH);
		newFilter.setImage(ImageRepository.getImage("new"));
		newFilter
				.setToolTipText("Clear all entries and allow for creation of new filter");
		newFilter.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				clearFilterControls();
				if (filterTable != null && !filterTable.isDisposed()) {
					filterTable.deselectAll();
				}
			}
		});

		createFilter = new ToolItem(tb, SWT.PUSH);
		createFilter.setImage(ImageRepository.getImage("create"));
		createFilter
				.setToolTipText("Create a new filter using provided information");
		createFilter.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (!filterName.getText().equals("")
						&& !filterPattern.getText().equals("")) {
					Filter f = new Filter(filterName.getText(), filterPattern
							.getText(),
							filterUseDownloadHistory.getSelection(), false); // TODO
					// real
					// HashDH
					f.setDenyPattern(filterDenyPattern.getText());
					f.setCategory(category.getText());
					f.setOutputDir(saveDir.getText());
					f.setRenameTo(renameTo.getText());
					f.setMoveAfterCompletion(filterMoveAfterCompletion
							.getSelection());
					if (!azsmrcUser.getText().equals("")) {
						f.setAzsmrcUser(azsmrcUser.getText());
					}
					if (Long.parseLong(filterMinSize.getText()) == -1) {
						f.setMinSize(-1);
					} else {
						f.setMinSize(Long.parseLong(filterMinSize.getText())
								* (long) Math.pow(1024, filterMinUnitSize
										.getSelectionIndex()));
					}
					if (Long.parseLong(filterMaxSize.getText()) == -1) {
						f.setMaxSize(-1);
					} else {
						f.setMaxSize(Long.parseLong(filterMaxSize.getText())
								* (long) Math.pow(1024, filterMaxUnitSize
										.getSelectionIndex()));
					}
					f
							.setInitialState(Filter.DownloadState.values()[initialStateCombo
									.getSelectionIndex()]);

					f.setRunOnce(bRunOnce.getSelection());

					if (filterUseDownloadHistory.getSelection()) {
						DownloadHistory dh = f.getDownloadHistory();

						if (filterMaxEp.getText().equals("")) {
							dh.setMaxEp(-1);
						} else {
							dh
									.setMaxEp(Integer.parseInt(filterMaxEp
											.getText()));
						}

						if (filterMinEp.getText().equals("")) {
							dh.setMinEp(-1);
						} else {
							dh
									.setMinEp(Integer.parseInt(filterMinEp
											.getText()));
						}

						if (filterMaxSeason.getText().equals("")
								&& filterMaxEp.getText().equals("")) {
							dh.setMaxSeason(-1);
						} else if (filterMaxSeason.getText().equals("")) {
							dh.setMaxSeason(1);
						} else {
							dh.setMaxSeason(Integer.parseInt(filterMaxSeason
									.getText()));
						}

						if (filterMinSeason.getText().equals("")
								&& filterMinEp.getText().equals("")) {
							dh.setMinSeason(-1);
						} else if (filterMinSeason.getText().equals("")) {
							dh.setMinSeason(1);
						} else {
							dh.setMinSeason(Integer.parseInt(filterMinSeason
									.getText()));
						}

						dh.setEpPatternMode(filterCustomDHMode);
						if (!filterCustomDHMode
								.equals(EpisodePatternMatching.Normal)) {
							dh.setCustomEpPattern(Pattern.compile(
									filterDHPattern.getText(),
									Pattern.CASE_INSENSITIVE));
						}
						dh.setDownloadHigherVersion(dhHistoryUseHigherVersion
								.getSelection());

					} else if (filterUseDownloadHistoryHash.getSelection()) {
						f.setUseHashDownloadHistory(true);
					} else {
						f.setUseDownloadHistory(false);
						f.setUseHashDownloadHistory(false);
					}

					parent.getFeedManager().addFilter(f);
					clearFilterControls();
					if (filterTable != null && !filterTable.isDisposed()) {
						filterTable.deselectAll();
					}
				}
			}
		});

		saveFilter = new ToolItem(tb, SWT.PUSH);
		saveFilter.setImage(ImageRepository.getImage("save"));
		saveFilter.setToolTipText("Save changes to Filter");
		saveFilter.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (filterSelected != null) {
					if (filterName.getText() != null
							&& filterPattern.getText() != null) {
						if (filterUseDownloadHistoryHash.getSelection()) {
							filterSelected.setUseHashDownloadHistory(true);
						} else if (filterUseDownloadHistory.getSelection()) {
							if (filterUseDownloadHistory.getSelection() != filterSelected
									.isUsingDownloadHistory()) {
								filterSelected
										.setUseDownloadHistory(filterUseDownloadHistory
												.getSelection());
							}

							DownloadHistory dh = filterSelected
									.getDownloadHistory();

							if (filterMaxEp.getText().equals("")) {
								dh.setMaxEp(-1);
							} else {
								dh.setMaxEp(Integer.parseInt(filterMaxEp
										.getText()));
							}

							if (filterMinEp.getText().equals("")) {
								dh.setMinEp(-1);
							} else {
								dh.setMinEp(Integer.parseInt(filterMinEp
										.getText()));
							}

							if (filterMaxSeason.getText().equals("")
									&& filterMaxEp.getText().equals("")) {
								dh.setMaxSeason(-1);
							} else if (filterMaxSeason.getText().equals("")) {
								dh.setMaxSeason(1);
							} else {
								dh.setMaxSeason(Integer
										.parseInt(filterMaxSeason.getText()));
							}

							if (filterMinSeason.getText().equals("")
									&& filterMinEp.getText().equals("")) {
								dh.setMinSeason(-1);
							} else if (filterMinSeason.getText().equals("")) {
								dh.setMinSeason(1);
							} else {
								dh.setMinSeason(Integer
										.parseInt(filterMinSeason.getText()));
							}

							dh.setEpPatternMode(filterCustomDHMode);
							if (!filterCustomDHMode
									.equals(EpisodePatternMatching.Normal)) {
								dh.setCustomEpPattern(Pattern.compile(
										filterDHPattern.getText(),
										Pattern.CASE_INSENSITIVE));
							}
							dh
									.setDownloadHigherVersion(dhHistoryUseHigherVersion
											.getSelection());

						} else {
							filterSelected.setUseDownloadHistory(false);
							filterSelected.setUseHashDownloadHistory(false);
						}
						filterSelected.setName(filterName.getText());
						filterSelected
								.setPatternString(filterPattern.getText());
						filterSelected.setDenyPattern(filterDenyPattern
								.getText());
						filterSelected.setCategory(category.getText());
						filterSelected.setOutputDir(saveDir.getText());
						filterSelected.setRenameTo(renameTo.getText());
						filterSelected
								.setMoveAfterCompletion(filterMoveAfterCompletion
										.getSelection());

						filterSelected.setAzsmrcUser(azsmrcUser.getText());

						if (Long.parseLong(filterMinSize.getText()) == -1) {
							filterSelected.setMinSize(-1);
						} else {
							filterSelected.setMinSize(Long
									.parseLong(filterMinSize.getText())
									* (long) Math.pow(1024, filterMinUnitSize
											.getSelectionIndex()));
						}

						if (Long.parseLong(filterMaxSize.getText()) == -1) {
							filterSelected.setMaxSize(-1);
						} else {
							filterSelected.setMaxSize(Long
									.parseLong(filterMaxSize.getText())
									* (long) Math.pow(1024, filterMaxUnitSize
											.getSelectionIndex()));
						}

						filterSelected.setRunOnce(bRunOnce.getSelection());

						filterSelected
								.setInitialState(Filter.DownloadState.values()[initialStateCombo
										.getSelectionIndex()]);

						parent.updateFilterTables();
					}
				}
			}
		});

		// Default it off so that no accedental overwrites happen
		saveFilter.setEnabled(false);

		cancelFilter = new ToolItem(tb, SWT.PUSH);
		cancelFilter.setImage(ImageRepository.getImage("cancel"));
		cancelFilter
				.setToolTipText("Cancel all changes and leave filter unmodified");
		cancelFilter.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				clearFilterControls();
				if (filterTable != null && !filterTable.isDisposed()) {
					filterTable.deselectAll();
				}
			}
		});

		deleteFilter = new ToolItem(tb, SWT.PUSH);
		deleteFilter.setImage(ImageRepository.getImage("delete"));
		deleteFilter.setToolTipText("Delete selected Filter");
		deleteFilter.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (filterSelected != null) {
					MessageBox mb = new MessageBox(tb.getShell(),
							SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					mb.setText("Delete Confirmation");
					mb
							.setMessage("Are you sure you wish to delete the filter: "
									+ filterSelected.getName());
					int answer = mb.open();
					if (answer == SWT.NO) {
						return;
					}

					parent.getFeedManager().removeFilter(filterSelected);
					clearFilterControls();
					if (filterTable != null && !filterTable.isDisposed()) {
						filterTable.deselectAll();
					}
				}
			}
		});

		new ToolItem(tb, SWT.SEPARATOR);

		addAllFilter = new ToolItem(tb, SWT.PUSH);
		addAllFilter.setImage(ImageRepository.getImage("dbAdd"));
		addAllFilter.setToolTipText("Add Filter to all Feeds.");
		addAllFilter.setEnabled(false);
		addAllFilter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				if (filterSelected != null) {
					parent.getFeedManager().assignFilterToAllFeeds(
							filterSelected);
					parent.updateFilterTables();
				}
			}
		});

		removeAllFilter = new ToolItem(tb, SWT.PUSH);
		removeAllFilter.setImage(ImageRepository.getImage("dbRemove"));
		removeAllFilter.setToolTipText("Remove Filter from all Feeds.");
		removeAllFilter.setEnabled(false);
		removeAllFilter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				if (filterSelected != null) {
					parent.getFeedManager().removeFilterFromAllFeeds(
							filterSelected);
					parent.updateFilterTables();
				}
			}
		});

		editFeedAssociation = new ToolItem(tb, SWT.PUSH);
		editFeedAssociation.setImage(ImageRepository.getImage("feed"));
		editFeedAssociation.setToolTipText("Edit Feed Associations");
		editFeedAssociation.setEnabled(false);
		editFeedAssociation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				if (filterSelected != null) {
					new FilterFeedAssociation(display, filterSelected, parent
							.getFeedManager());

				}
			}
		});

	}

	private void createFilterGroup (Composite parentComp) {
		Group filterGroup = new Group(parentComp, SWT.NULL);
		filterGroup.setText("Filter");
		GridLayout gl = new GridLayout();
		gl.numColumns = 2;
		filterGroup.setLayout(gl);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		filterGroup.setLayoutData(gridData);

		Label filterNameLabel = new Label(filterGroup, SWT.NULL);
		filterNameLabel.setText("Name:");

		filterName = new Text(filterGroup, SWT.BORDER);
		filterName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label filterPatternLabel = new Label(filterGroup, SWT.NULL);
		filterPatternLabel.setText("Accept Pattern:");

		filterPattern = new Text(filterGroup, SWT.BORDER);
		filterPattern.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label filterDenyPatternLabel = new Label(filterGroup, SWT.NULL);
		filterDenyPatternLabel.setText("Deny Pattern:");

		filterDenyPattern = new Text(filterGroup, SWT.BORDER);
		filterDenyPattern.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		filterDenyPattern
				.setToolTipText("Input a desired rejection pattern.  This field is not required");
		// verify listener
		Listener verifyListener = new Listener() {
			public void handleEvent (Event e) {
				final Text text = (Text) e.widget;
				try {
					Pattern.compile(text.getText(), Pattern.CASE_INSENSITIVE);

					// Pattern passes
					text.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
					createFilter.setEnabled(true);
					saveFilter.setEnabled(true);

				} catch (PatternSyntaxException pse) {
					// Pattern fails!
					text.setForeground(display
							.getSystemColor(SWT.COLOR_DARK_RED));
					createFilter.setEnabled(false);
					saveFilter.setEnabled(false);
				}
			}
		};
		filterPattern.addListener(SWT.Modify, verifyListener);
		filterDenyPattern.addListener(SWT.Modify, verifyListener);

		// --Rule Builder Button
		Button bRuleBuilder = new Button(filterGroup, SWT.PUSH);
		bRuleBuilder.setText("Pattern Assistant");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		bRuleBuilder.setLayoutData(gridData);
		bRuleBuilder.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				GUIPatternAssistant.open(parent, filterPattern.getText(),
						filterDenyPattern.getText(), display, parent
								.getFeedManager().getFeedsAsArray());
			}
		});
	}

	private void createDownloadHistoryGroup (Composite parentComp) {

		Group dhGroup = new Group(parentComp, SWT.NULL);
		dhGroup.setText("Download History");
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		dhGroup.setLayoutData(gridData);
		dhGroup.setLayout(new GridLayout(4, false));

		filterUseDownloadHistoryNone = new Button(dhGroup, SWT.RADIO);
		filterUseDownloadHistoryNone.setText("Do not use Download History");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 4;
		filterUseDownloadHistoryNone.setLayoutData(gridData);
		filterUseDownloadHistoryNone.setSelection(true);
		filterUseDownloadHistoryNone.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				// TODO
			}
		});

		filterUseDownloadHistoryHash = new Button(dhGroup, SWT.RADIO);
		filterUseDownloadHistoryHash.setText("Use Hash Based Download History");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 4;
		filterUseDownloadHistoryHash.setLayoutData(gridData);
		filterUseDownloadHistoryHash.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				// TODO
			}
		});

		filterUseDownloadHistory = new Button(dhGroup, SWT.RADIO);
		filterUseDownloadHistory.setText("Use Download History");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 4;
		filterUseDownloadHistory.setLayoutData(gridData);
		filterUseDownloadHistory.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				setNormalDownloadHistory(filterUseDownloadHistory
						.getSelection());
			}
		});

		Composite dhSettingsComp = new Composite(dhGroup, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 4;
		dhSettingsComp.setLayoutData(gridData);
		dhSettingsComp.setLayout(new GridLayout(4, false));

		// Min Season
		filterMinSeason = new Text(dhSettingsComp, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 50;
		filterMinSeason.setLayoutData(gridData);

		filterMinSeasonL = new Label(dhSettingsComp, SWT.NULL);
		filterMinSeasonL.setText("Min Season");

		// Min Ep
		filterMinEp = new Text(dhSettingsComp, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 50;
		filterMinEp.setLayoutData(gridData);

		filterMinEpL = new Label(dhSettingsComp, SWT.NULL);
		filterMinEpL.setText("Min Episode");

		// Max Season
		filterMaxSeason = new Text(dhSettingsComp, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 50;
		filterMaxSeason.setLayoutData(gridData);

		filterMaxSeasonL = new Label(dhSettingsComp, SWT.NULL);
		filterMaxSeasonL.setText("Max Season");

		// Max Episode
		filterMaxEp = new Text(dhSettingsComp, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 50;
		filterMaxEp.setLayoutData(gridData);

		filterMaxEpL = new Label(dhSettingsComp, SWT.NULL);
		filterMaxEpL.setText("Max Episode");

		// Download History Button
		dlHistory = new Button(dhSettingsComp, SWT.PUSH);
		gridData = new GridData();
		gridData.horizontalSpan = 4;
		dlHistory.setLayoutData(gridData);
		dlHistory.setText("View Download History");
		dlHistory.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (filterSelected != null
						&& filterSelected.getDownloadHistory() != null) {
					GUI_DownloadHistory.open(display, filterSelected
							.getDownloadHistory());
				}
			}
		});

		/*
		 * private Label filterDHMode, filterDHPattern; private Button
		 * filterDHInputDialog;
		 */

		Label modeLabel = new Label(dhSettingsComp, SWT.NULL);
		modeLabel.setText("Mode:");

		filterDHMode = new Label(dhSettingsComp, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		filterDHMode.setLayoutData(gridData);
		filterDHMode.setText(filterCustomDHMode.toString());

		Label patternLabel = new Label(dhSettingsComp, SWT.NULL);
		patternLabel.setText("Pattern:");

		filterDHPattern = new Label(dhSettingsComp, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		filterDHPattern.setLayoutData(gridData);

		filterDHInputDialog = new Button(dhSettingsComp, SWT.PUSH);
		filterDHInputDialog.setText("Edit Custom Pattern");
		gridData = new GridData();
		gridData.horizontalSpan = 4;
		filterDHInputDialog.setLayoutData(gridData);
		filterDHInputDialog.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				final Shell dialog = new Shell(SWT.DIALOG_TRIM
						| SWT.APPLICATION_MODAL);
				dialog.setLayout(new GridLayout(1, false));
				dialog.setText("Custom Pattern Editor");

				Composite comp = new Composite(dialog, SWT.NULL);
				comp.setLayout(new GridLayout(3, false));
				GridData gd = new GridData(GridData.FILL_BOTH);
				comp.setLayoutData(gd);

				Label typeL = new Label(comp, SWT.NULL);
				typeL.setText("Mode:");

				final Combo typeCombo = new Combo(comp, SWT.DROP_DOWN
						| SWT.READ_ONLY);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = 1;
				gd.widthHint = 300;
				typeCombo.setLayoutData(gd);
				for (EpisodePatternMatching epm : EpisodePatternMatching
						.values()) {
					typeCombo.add(epm.name());
					if (filterSelected != null
							&& filterSelected.getDownloadHistory() != null) {
						if (filterSelected.getDownloadHistory()
								.getEpPatternMode().equals(epm)) {
							typeCombo.select(typeCombo.getItemCount() - 1);
						}
					} else {
						typeCombo.select(0);
					}

				}

				Button defaultButton = new Button(comp, SWT.PUSH);
				defaultButton.setText("Show Default Settings");
				defaultButton
						.setToolTipText("Click this button to show suggested settings for the chosen Mode");

				Label testL = new Label(comp, SWT.NULL);
				testL.setText("Test Data:");

				final Text testText = new Text(comp, SWT.SINGLE | SWT.BORDER);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = 2;
				testText.setLayoutData(gd);

				Label patternL = new Label(comp, SWT.NULL);
				patternL.setText("Pattern Input:");

				final Text patternText = new Text(comp, SWT.SINGLE | SWT.BORDER);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = 2;
				patternText.setLayoutData(gd);

				Label outputL = new Label(comp, SWT.NULL);
				outputL.setText("Output:");
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = 3;
				outputL.setLayoutData(gd);

				final Text outputText = new Text(comp, SWT.MULTI | SWT.BORDER
						| SWT.READ_ONLY);
				gd = new GridData(GridData.FILL_BOTH);
				gd.horizontalSpan = 3;
				gd.verticalSpan = 30;
				outputText.setLayoutData(gd);

				// Set up Default button workings here
				defaultButton.addListener(SWT.Selection, new Listener() {
					public void handleEvent (Event arg0) {
						EpisodePatternMatching mode = EpisodePatternMatching
								.values()[typeCombo.getSelectionIndex()];
						switch (mode) {
						case Normal:
							patternText.setText("");
							break;
						case Episode:
							patternText.setText("(\\d{1,3})");
							break;
						case Episode_Episode:
							patternText.setText("(\\d{1,3}).*?(\\d{1,3})");
							break;
						case Season_Episode:
							patternText.setText("(\\d{1,3}).*?(\\d{1,3})");
							break;
						case Season_Episode_Episode:
							patternText
									.setText("(\\d{1,3}).*?(\\d{1,3}).*?(\\d{1,3})");
							break;
						case Season_Episode_Season_Episode:
							patternText
									.setText("(\\d{1,3}).*?(\\d{1,3}).*?(\\d{1,3}).*?(\\d{1,3})");
							break;
						}
					}

				});

				typeCombo.addSelectionListener(new SelectionAdapter() {
					/*
					 * (non-Javadoc)
					 * 
					 * @see
					 * org.eclipse.swt.events.SelectionAdapter#widgetSelected
					 * (org.eclipse.swt.events.SelectionEvent)
					 */
					@Override
					public void widgetSelected (SelectionEvent e) {
						if (patternText.getText().equals("")
								|| patternText.getText().equals("(\\d{1,3})")
								|| patternText.getText().equals(
										"(\\d{1,3}).*?(\\d{1,3})")
								|| patternText.getText().equals(
										"(\\d{1,3}).*?(\\d{1,3}).*?(\\d{1,3})")
								|| patternText
										.getText()
										.equals(
												"(\\d{1,3}).*?(\\d{1,3}).*?(\\d{1,3}).*?(\\d{1,3})")) {

							EpisodePatternMatching mode = EpisodePatternMatching
									.values()[typeCombo.getSelectionIndex()];
							switch (mode) {
							case Normal:
								patternText.setText("");
								break;
							case Episode:
								patternText.setText("(\\d{1,3})");
								break;
							case Episode_Episode:
								patternText.setText("(\\d{1,3}).*?(\\d{1,3})");
								break;
							case Season_Episode:
								patternText.setText("(\\d{1,3}).*?(\\d{1,3})");
								break;
							case Season_Episode_Episode:
								patternText
										.setText("(\\d{1,3}).*?(\\d{1,3}).*?(\\d{1,3})");
								break;
							case Season_Episode_Season_Episode:
								patternText
										.setText("(\\d{1,3}).*?(\\d{1,3}).*?(\\d{1,3}).*?(\\d{1,3})");
								break;
							}
						}
					}

				});

				Composite bComp = new Composite(comp, SWT.NULL);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = 3;
				bComp.setLayoutData(gd);
				bComp.setLayout(new GridLayout(3, false));

				final Button bTest = new Button(bComp, SWT.PUSH);
				bTest.setText("Test");
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
				bTest.setLayoutData(gd);
				bTest.addListener(SWT.Selection, new Listener() {
					public void handleEvent (Event arg0) {
						EpisodePatternMatching mode = EpisodePatternMatching
								.values()[typeCombo.getSelectionIndex()];
						Episode e = new Episode(testText.getText(), mode,
								Pattern.compile(patternText.getText(),
										Pattern.CASE_INSENSITIVE));
						switch (mode) {
						case Normal:
							outputText.setText("");
							break;
						case Episode:
							outputText.setText("Episode: " + e.getEpisode());
							break;
						case Episode_Episode:
							outputText.setText("Episode: " + e.getEpisode()
									+ " - Episode: " + e.getEpisode_end());
							break;
						case Season_Episode:
							outputText.setText("Season: " + e.getSeason()
									+ " Episode: " + e.getEpisode());
							break;
						case Season_Episode_Episode:
							outputText.setText("Season: " + e.getSeason()
									+ " Episode: " + e.getEpisode()
									+ " - Episode: " + e.getEpisode_end());
							break;
						case Season_Episode_Season_Episode:
							outputText.setText("Season: " + e.getSeason()
									+ " Episode: " + e.getEpisode()
									+ " - Season: " + e.getSeason()
									+ " Episode: " + e.getEpisode_end());
							break;
						}

					}
				});

				final Button bSave = new Button(bComp, SWT.PUSH);
				bSave.setText("Save");
				bSave
						.setLayoutData(new GridData(
								GridData.HORIZONTAL_ALIGN_END));
				bSave.addListener(SWT.Selection, new Listener() {

					public void handleEvent (Event arg0) {
						filterCustomDHMode = EpisodePatternMatching.values()[typeCombo
								.getSelectionIndex()];
						if (filterCustomDHMode
								.equals(EpisodePatternMatching.Normal)) {
							filterDHPattern.setText("");
						} else {
							filterDHPattern.setText(patternText.getText());
						}
						filterDHMode.setText(filterCustomDHMode.toString());

						dialog.close();
					}
				});

				final Button bCancel = new Button(bComp, SWT.PUSH);
				bCancel.setText("Cancel");
				bCancel.setLayoutData(new GridData(
						GridData.HORIZONTAL_ALIGN_END));
				bCancel.addListener(SWT.Selection, new Listener() {

					public void handleEvent (Event arg0) {
						dialog.close();
					}
				});

				// blank out if Normal
				if (filterSelected != null
						&& filterSelected.getDownloadHistory() != null) {
					if (filterSelected.getDownloadHistory().getEpPatternMode()
							.equals(EpisodePatternMatching.Normal)) {
						patternText.setEnabled(false);
						patternText.setText("");
					} else {
						patternText.setEnabled(true);
						patternText.setText(filterSelected.getDownloadHistory()
								.getCustomEpPattern().pattern());
					}

				}

				// combo listener
				typeCombo.addListener(SWT.Selection, new Listener() {
					public void handleEvent (Event arg0) {
						if (typeCombo.getSelectionIndex() == 0) {
							patternText.setEnabled(false);

						} else {
							patternText.setEnabled(true);
						}

					}

				});

				// pattern verify listener
				patternText.addModifyListener(new ModifyListener() {
					/*
					 * (non-Javadoc)
					 * 
					 * @see
					 * org.eclipse.swt.events.ModifyListener#modifyText(org.
					 * eclipse.swt.events.ModifyEvent)
					 */
					public void modifyText (ModifyEvent e) {
						final Text text = (Text) e.widget;
						try {
							Matcher m = Pattern.compile(text.getText(),
									Pattern.CASE_INSENSITIVE).matcher(
									testText.getText());

							text.setForeground(display
									.getSystemColor(SWT.COLOR_BLACK));
							bSave.setEnabled(true);
							bTest.setEnabled(true);
							String s = "";
							if (m.find()) {
								for (int i = 0; i <= m.groupCount(); i++) {
									s += "Group (" + i + ") = " + m.group(i)
											+ "\n";
								}
								outputText.setText(s);
							} else {
								outputText.setText("No Match found.");
							}
						} catch (final PatternSyntaxException pse) {
							// Pattern fails!
							text.setForeground(display
									.getSystemColor(SWT.COLOR_DARK_RED));
							bSave.setEnabled(false);
							bTest.setEnabled(false);
							outputText.setText(pse.getMessage());
							outputText.setForeground(display
									.getSystemColor(SWT.COLOR_DARK_RED));

						}
					}
				});

				// open shell
				dialog.pack();

				// Center Shell
				Monitor primary = dialog.getDisplay().getPrimaryMonitor();
				Rectangle bounds = primary.getBounds();
				Rectangle rect = dialog.getBounds();
				int x = bounds.x + (bounds.width - rect.width) / 2;
				int y = bounds.y + (bounds.height - rect.height) / 2;
				dialog.setLocation(x, y);

				// open shell
				dialog.open();
			}
		});

		dhHistoryUseHigherVersion = new Button(dhSettingsComp, SWT.CHECK);
		dhHistoryUseHigherVersion.setText("Download higher Versions");
		dhHistoryUseHigherVersion
				.setToolTipText("If checked, episodes will be downloaded if their version is higher\n"
						+ "than the already downloaded one.\n e.g. v2 == Version 2. 'proper' is treated as Version 2.");

		// All off by default
		setNormalDownloadHistory(false);

	}

	private void createAdvancedOptions (Composite parentComp) {
		final Button useAdvBtn = new Button(parentComp, SWT.CHECK);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		useAdvBtn.setLayoutData(gridData);
		useAdvBtn.setText("Show Advanced Options");
		useAdvBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (advGroup != null && !advGroup.isDisposed()) {
					advGroup.setVisible(useAdvBtn.getSelection());
				}
			}
		});

		advGroup = new Group(parentComp, SWT.NULL);
		advGroup.setText("Advanced Options");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		advGroup.setLayoutData(gridData);
		advGroup.setLayout(new GridLayout(2, false));

		Label noteLabel = new Label(advGroup, SWT.NULL);
		noteLabel
				.setText("Options prefixed with * are only available in the Azureus version.");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		noteLabel.setLayoutData(gridData);

		bRunOnce = new Button(advGroup, SWT.CHECK);
		bRunOnce.setText("Only run filter once");
		bRunOnce
				.setToolTipText("If enabled, the filter will run once, then the filter will be disabled");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		bRunOnce.setLayoutData(gridData);

		Label minSizeLabel = new Label(advGroup, SWT.None);
		minSizeLabel
				.setText("Minimum Size of the Download (-1 to deactivate):");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		minSizeLabel.setLayoutData(gridData);

		filterMinSize = new Text(advGroup, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		filterMinSize.setLayoutData(gridData);
		filterMinSize.setText("-1");

		filterMinUnitSize = new Combo(advGroup, SWT.READ_ONLY);
		filterMinUnitSize.add("Byte");
		filterMinUnitSize.add("KByte");
		filterMinUnitSize.add("MByte");
		filterMinUnitSize.add("GByte");
		filterMinUnitSize.select(0);

		Label maxSizeLabel = new Label(advGroup, SWT.None);
		maxSizeLabel
				.setText("Maximum Size of the Download (-1 to deactivate):");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		maxSizeLabel.setLayoutData(gridData);

		filterMaxSize = new Text(advGroup, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		filterMaxSize.setLayoutData(gridData);
		filterMaxSize.setText("-1");

		filterMaxUnitSize = new Combo(advGroup, SWT.READ_ONLY);
		filterMaxUnitSize.add("Byte");
		filterMaxUnitSize.add("KByte");
		filterMaxUnitSize.add("MByte");
		filterMaxUnitSize.add("GByte");
		filterMaxUnitSize.select(0);

		Label categoryLabel = new Label(advGroup, SWT.NULL);
		categoryLabel.setText("*Category or Tag(s) [comma separated]:");

		category = new Text(advGroup, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		category.setLayoutData(gridData);

		Label savDirLabel = new Label(advGroup, SWT.NULL);
		savDirLabel.setText("Save Dir:");

		saveDir = new Text(advGroup, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		saveDir.setLayoutData(gridData);
		saveDir
				.setToolTipText("{FilterName} inserts the name of the filter.\n{S} or {SS} will insert the Season number.");

		filterMoveAfterCompletion = new Button(advGroup, SWT.CHECK);
		filterMoveAfterCompletion
				.setText("Move the Download after Completion.");
		filterMoveAfterCompletion
				.setToolTipText("This will move the Download to Save Dir, after it finishes.");

		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		filterMoveAfterCompletion.setLayoutData(gridData);

		Label renameToLabel = new Label(advGroup, SWT.NULL);
		renameToLabel.setText("Rename To:");

		renameTo = new Text(advGroup, SWT.SINGLE | SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		renameTo.setLayoutData(gridData);
		renameTo
				.setToolTipText("{FilterName} inserts the name of the filter.\n{S} or {SS} will insert the Season number,\n"
						+ "{E}, {EE} or {EEE} will insert the Episode number with zerofill.\nExtension will be untouched.");

		Label hBar = new Label(advGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		hBar.setLayoutData(gridData);

		Label initialStateLabel = new Label(advGroup, SWT.NULL);
		initialStateLabel.setText("*Import as:");
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		initialStateLabel.setLayoutData(gridData);

		initialStateCombo = new Combo(advGroup, SWT.READ_ONLY | SWT.FLAT);
		for (Filter.DownloadState s : Filter.DownloadState.values()) {
			initialStateCombo.add(I18N.translate(FILTER_PFX + ".initialstate."
					+ s.name()));
		}
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		initialStateCombo.setLayoutData(gridData);
		initialStateCombo.select(0);

		Label hBar2 = new Label(advGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		hBar2.setLayoutData(gridData);

		Label azsmrcLabel = new Label(advGroup, SWT.NULL);
		azsmrcLabel
				.setText("AzSMRC users can import a torrent as a specific user\nNote: User name is case sensitive."
						+ " and needs to be exact");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		azsmrcLabel.setLayoutData(gridData);

		Label azsmrcUserLabel = new Label(advGroup, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		azsmrcUserLabel.setLayoutData(gridData);
		azsmrcUserLabel.setText("*AzSMRC User:");

		azsmrcUser = new Combo(advGroup, SWT.BORDER);
		azsmrcUser.setItems(parent.getAzSMRCUsernames());
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		azsmrcUser.setLayoutData(gridData);

		// Default advGroup to invisible and useAdvBtn to off
		useAdvBtn.setSelection(false);
		advGroup.setVisible(false);
	}

	/**
	 * Sets the Normal Download History Enabled or Disabled in the Filter Tab
	 * 
	 * @param _bEnabled
	 */
	private void setNormalDownloadHistory (final boolean _bEnabled) {
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(new SWTSafeRunnable() {
			@Override
			public void runSafe () {
				filterMinSeason.setEnabled(_bEnabled);
				filterMaxSeason.setEnabled(_bEnabled);
				filterMinEp.setEnabled(_bEnabled);
				filterMaxEp.setEnabled(_bEnabled);
				dlHistory.setEnabled(_bEnabled);
				filterDHInputDialog.setEnabled(_bEnabled);
				dhHistoryUseHigherVersion.setEnabled(_bEnabled);
				if (!_bEnabled) {
					filterMaxSeason.setText("");
					filterMinSeason.setText("");
					filterMaxEp.setText("");
					filterMinEp.setText("");
					filterDHMode.setText("");
					filterDHPattern.setText("");
					dhHistoryUseHigherVersion.setSelection(false);
				}
			}
		});
	}

	/**
	 * Resets all filter controls
	 */
	protected void clearFilterControls () {
		filterMaxEp.setText("");
		filterMinEp.setText("");
		filterMaxSeason.setText("");
		filterMinSeason.setText("");
		filterName.setText("");
		filterPattern.setText("");
		filterDenyPattern.setText("");
		azsmrcUser.setText("");
		filterUseDownloadHistory.setSelection(false);
		filterUseDownloadHistoryNone.setSelection(true);
		filterUseDownloadHistoryHash.setSelection(false);
		category.setText("");
		saveDir.setText("");
		renameTo.setText("");
		filterMoveAfterCompletion.setSelection(false);
		filterCustomDHMode = EpisodePatternMatching.Normal;
		filterDHMode.setText(EpisodePatternMatching.Normal.toString());
		filterDHPattern.setText("");
		dhHistoryUseHigherVersion.setSelection(false);
		initialStateCombo.select(0);
		filterMinSize.setText("-1");
		filterMaxSize.setText("-1");
		filterMinUnitSize.select(0);
		filterMaxUnitSize.select(0);
		setNormalDownloadHistory(false);
	}

	private void selectFilter (Filter f) {

		saveFilter.setEnabled(true);
		deleteFilter.setEnabled(true);
		addAllFilter.setEnabled(true);
		removeAllFilter.setEnabled(true);
		editFeedAssociation.setEnabled(true);
		filterSelected = f;
		filterName.setText(f.getName());
		filterPattern.setText(f.getPatternString());
		if (f.getDenyPatternString() != null) {
			filterDenyPattern.setText(f.getDenyPatternString());
		} else {
			filterDenyPattern.setText("");
		}
		if (f.getOutputDir() != null) {
			saveDir.setText(f.getOutputDir());
		} else {
			saveDir.setText("");
		}
		if (f.getRenameTo() != null) {
			renameTo.setText(f.getRenameTo());
		} else {
			renameTo.setText("");
		}
		if (f.getCategory() != null) {
			category.setText(f.getCategory());
		} else {
			category.setText("");
		}
		bRunOnce.setSelection(f.isRunOnce());
		filterMoveAfterCompletion.setSelection(f.isMoveAfterCompletion());

		// import torrent setting
		initialStateCombo.select(f.getInitialState().ordinal());

		if (f.getMinSize() == -1) {
			filterMinSize.setText("-1");
			filterMinUnitSize.select(0);
		} else {
			filterMinSize.setText(Long.toString(f.getMinSize()));
			if (f.getMinSize() >= 1073741824
					&& f.getMinSize() % 1073741824 == 0) {
				filterMinUnitSize.select(3);
				filterMinSize.setText(Long
						.toString(f.getMinSize() / 1073741824));
			} else if (f.getMinSize() >= 1048576
					&& f.getMinSize() % 1048576 == 0) {
				filterMinUnitSize.select(2);
				filterMinSize.setText(Long.toString(f.getMinSize() / 1048576));
			} else if (f.getMinSize() >= 1024 && f.getMinSize() % 1024 == 0) {
				filterMinUnitSize.select(1);
				filterMinSize.setText(Long.toString(f.getMinSize() / 1024));
			} else {
				filterMinUnitSize.select(0);
				filterMinSize.setText(Long.toString(f.getMinSize()));
			}
		}

		if (f.getMaxSize() == -1) {
			filterMaxSize.setText("-1");
			filterMaxUnitSize.select(0);
		} else {
			filterMaxSize.setText(Long.toString(f.getMaxSize()));
			if (f.getMaxSize() >= 1073741824
					&& f.getMaxSize() % 1073741824 == 0) {
				filterMaxUnitSize.select(3);
				filterMaxSize.setText(Long
						.toString(f.getMaxSize() / 1073741824));
			} else if (f.getMaxSize() >= 1048576
					&& f.getMaxSize() % 1048576 == 0) {
				filterMaxUnitSize.select(2);
				filterMaxSize.setText(Long.toString(f.getMaxSize() / 1048576));
			} else if (f.getMaxSize() >= 1024 && f.getMaxSize() % 1024 == 0) {
				filterMaxUnitSize.select(1);
				filterMaxSize.setText(Long.toString(f.getMaxSize() / 1024));
			} else {
				filterMaxUnitSize.select(0);
				filterMaxSize.setText(Long.toString(f.getMaxSize()));
			}
		}

		azsmrcUser
				.setText((f.getAzsmrcUser() == null) ? "" : f.getAzsmrcUser());

		if (f.isUsingDownloadHistory()) {
			filterUseDownloadHistory.setSelection(true);
			filterUseDownloadHistoryNone.setSelection(false);
			filterUseDownloadHistoryHash.setSelection(false);
			DownloadHistory dh = f.getDownloadHistory();
			filterMaxSeason.setText(Integer.toString(dh.getMaxSeason()));
			filterMinSeason.setText(Integer.toString(dh.getMinSeason()));
			filterMaxEp.setText(Integer.toString(dh.getMaxEp()));
			filterMinEp.setText(Integer.toString(dh.getMinEp()));
			filterDHMode.setText(dh.getEpPatternMode().toString());
			if (!dh.getEpPatternMode().equals(EpisodePatternMatching.Normal)) {
				filterDHPattern.setText(dh.getCustomEpPattern().pattern());
			} else {
				filterDHPattern.setText("");
			}
			dhHistoryUseHigherVersion
					.setSelection(dh.isDownloadHigherVersion());

			setNormalDownloadHistory(true);
		} else if (f.isUsingHashDownloadHistory()) {
			filterUseDownloadHistory.setSelection(false);
			filterUseDownloadHistoryNone.setSelection(false);
			filterUseDownloadHistoryHash.setSelection(true);
			setNormalDownloadHistory(false);
		} else {
			filterUseDownloadHistoryNone.setSelection(true);
			filterUseDownloadHistory.setSelection(false);
			filterUseDownloadHistoryHash.setSelection(false);
			setNormalDownloadHistory(false);
		}

	}

	protected void reset () {
		if (filterTable != null && !filterTable.isDisposed()) {
			filterTable.removeAll();
		}
	}

	protected void update () {
		if (filterTable != null && !filterTable.isDisposed()) {
			filterTable.removeAll();
			filterTable.setItemCount(parent.filterArray.length);
		}
	}

	protected void updateFilterTables () {
		if (display == null || display.isDisposed() || filterTable == null
				|| filterTable.isDisposed()) {
			return;
		}
		display.asyncExec(new SWTSafeRunnable() {
			@Override
			public void runSafe () {
				saveFilter.setEnabled(false);
				deleteFilter.setEnabled(false);
				createFilter.setEnabled(false);
				filterTable.clearAll();
				filterTable.setItemCount(parent.filterArray.length);
			}
		});
	}

	public void setPatternText (final String name, final String newPattern,
			final String newDenyPattern) {
		display.asyncExec(new SWTSafeRunnable() {

			@Override
			public void runSafe () {
				if (name != null) {
					filterName.setText(name);
				}
				filterPattern.setText(newPattern);
				filterDenyPattern.setText(newDenyPattern);
			}
		});
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
