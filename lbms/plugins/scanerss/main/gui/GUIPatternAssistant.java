package lbms.plugins.scanerss.main.gui;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lbms.plugins.scanerss.main.Filter;
import lbms.plugins.scanerss.main.RSSFeed;
import lbms.plugins.scanerss.main.RSSItem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class GUIPatternAssistant {

	private static GUIPatternAssistant	instance;
	private Shell						shell;
	private Text						patternText, denyText;
	private Text						fileNameText;
	private Text						tName, tGroup;
	private RSSFeed[]					feeds;
	private Table						matchesTable;
	private Display						display;
	private static Pattern				groupPattern	= Pattern
																.compile(
																		".*?\\[(.+?)\\].*?",
																		Pattern.CASE_INSENSITIVE);
	private Button						bSave, bCreate;
	private List<RSSItem>				items;
	private List<RSSItem>				filteredItems;

	private String						patternString	= "", denyString = "";

	/**
	 * A dialog that allows the user to test their pattern on all of the current
	 * feed urls/names
	 * 
	 * @param _feeds
	 */
	private GUIPatternAssistant(final GUI gui, String currentPattern,
			String currentDenyPattern, final Display _display,
			final RSSFeed[] _feeds) {
		patternString = currentPattern;
		denyString = currentDenyPattern;
		createContents(gui, _display, _feeds);
	}

	private GUIPatternAssistant(final GUI gui, RSSItem item,
			final Display _display, final RSSFeed[] _feeds) {
		createContents(gui, _display, _feeds);
		tName.setText(suggestName(item.getTitle()));
		tGroup.setText(suggestGroup(item.getTitle()));
	}

	private void createContents (final GUI gui, final Display _display,
			final RSSFeed[] _feeds) {
		feeds = _feeds;
		display = _display;
		// set the instance
		instance = this;

		// the main shell
		shell = new Shell(display, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setLayout(new GridLayout(1, false));
		shell.setText("Pattern Assistant");

		// the parent composite
		Composite parent = new Composite(shell, SWT.NULL);
		parent.setLayout(new GridLayout(1, false));

		GridData gd = new GridData(GridData.FILL_BOTH);
		parent.setLayoutData(gd);

		// The sash for the tables
		SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
		gd = new GridData(GridData.FILL_BOTH);
		sash.setLayoutData(gd);
		sash.setLayout(new GridLayout(3, false));

		Composite leftTableGroup = new Composite(sash, SWT.None);
		gd = new GridData(GridData.FILL_BOTH);
		leftTableGroup.setLayoutData(gd);
		leftTableGroup.setLayout(new GridLayout(2, false));

		Label filterLabel = new Label(leftTableGroup, SWT.NONE);
		filterLabel.setText("Filter:");

		final Text filterText = new Text(leftTableGroup, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		filterText.setLayoutData(gd);

		// part 1 of the sash -- the total talble
		final Table totalTable = new Table(leftTableGroup, SWT.SINGLE
				| SWT.FULL_SELECTION | SWT.BORDER | SWT.VIRTUAL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		totalTable.setLayoutData(gd);
		totalTable.setHeaderVisible(true);

		totalTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected (SelectionEvent e) {
				TableItem[] selection = totalTable.getSelection();
				if (selection.length != 1) {
					return;
				}
				RSSItem item = (RSSItem) selection[0].getData("RSSItem");
				tName.setText(suggestName(item.getTitle()));
				tGroup.setText(suggestGroup(item.getTitle()));
			}
		});

		TableColumn name = new TableColumn(totalTable, SWT.LEFT);
		name.setText("Name");
		name.setWidth(150);

		TableColumn url = new TableColumn(totalTable, SWT.LEFT);
		url.setText("URL");
		url.setWidth(150);

		items = new ArrayList<RSSItem>();
		// Fill the table with the feeds
		for (RSSFeed feed : feeds) {
			items.addAll(feed.getItems());
		}
		filteredItems = new ArrayList<RSSItem>(items);
		totalTable.setItemCount(filteredItems.size());

		totalTable.addListener(SWT.SetData, new Listener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			 */
			public void handleEvent (Event event) {
				TableItem item = (TableItem) event.item;
				int index = totalTable.indexOf(item);
					// meh, seen out-of-bounds here
				if ( index >= 0 && index < filteredItems.size()){
					RSSItem rss = filteredItems.get(index);
					item.setText(0, rss.getTitle());
					item.setText(1, rss.getLink());
					item.setData("RSSItem", rss);
				}
			}
		});

		filterText.addModifyListener(new ModifyListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
			public void modifyText (ModifyEvent e) {
				String text = filterText.getText();
				filterItems(text);
				totalTable.clearAll();
				totalTable.setItemCount(filteredItems.size());
			}
		});

		// part2 of the sash -- matches
		matchesTable = new Table(sash, SWT.SINGLE | SWT.FULL_SELECTION
				| SWT.BORDER);
		gd = new GridData(GridData.FILL_VERTICAL);
		matchesTable.setLayoutData(gd);
		matchesTable.setHeaderVisible(true);

		TableColumn matches = new TableColumn(matchesTable, SWT.LEFT);
		matches.setText("Matched Names");
		matches.setWidth(150);

		TableColumn matchedURL = new TableColumn(matchesTable, SWT.LEFT);
		matchedURL.setText("URL");
		matchedURL.setWidth(150);

		// part3 of the sash -- the right composit
		Composite rComp = new Composite(sash, SWT.BORDER);
		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = 0;
		gl.marginWidth = 5;
		rComp.setLayout(gl);

		gd = new GridData(GridData.FILL_BOTH);
		rComp.setLayoutData(gd);

		// Composite for input and buttons
		Composite inputComp = new Composite(rComp, SWT.NULL);
		inputComp.setLayout(new GridLayout(1, false));

		gd = new GridData(GridData.FILL_BOTH);
		inputComp.setLayoutData(gd);

		Label ruleLabel = new Label(inputComp, SWT.NULL);
		ruleLabel.setText("Input Test Rule:");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		ruleLabel.setLayoutData(gd);

		patternText = new Text(inputComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		patternText.setLayoutData(gd);
		patternText.setText(patternString);

		Label denyLabel = new Label(inputComp, SWT.NULL);
		denyLabel.setText("Input Deny Rule:");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		denyLabel.setLayoutData(gd);

		denyText = new Text(inputComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		denyText.setLayoutData(gd);
		denyText.setText(denyString);

		Button bTest = new Button(inputComp, SWT.PUSH);
		bTest.setText("Test Rule");
		bTest.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				testInput();
			}
		});

		final Label regexErrorLabel = new Label(inputComp, SWT.NONE);

		ModifyListener modifyListener = new ModifyListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
			public void modifyText (ModifyEvent e) {
				final Text text = (Text) e.widget;
				try {
					Pattern.compile(text.getText(), Pattern.CASE_INSENSITIVE);

					text.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
					bSave.setEnabled(true);
					bCreate.setEnabled(true);
					regexErrorLabel.setText("");
					regexErrorLabel.pack();

				} catch (final PatternSyntaxException pse) {
					// Pattern fails!
					text.setForeground(display
							.getSystemColor(SWT.COLOR_DARK_RED));
					bSave.setEnabled(false);
					bCreate.setEnabled(false);
					regexErrorLabel.setText(pse.getMessage());
					regexErrorLabel.setForeground(display
							.getSystemColor(SWT.COLOR_DARK_RED));
					regexErrorLabel.pack();
				}
			}
		};

		// modify listener

		patternText.addModifyListener(modifyListener);
		denyText.addModifyListener(modifyListener);

		// RegEx rule builder group
		Group gRB = new Group(rComp, SWT.NULL);
		gRB.setText("RegEx Rule Builder");
		gRB.setLayout(new GridLayout(2, false));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gRB.setLayoutData(gd);

		// Name
		Label lName = new Label(gRB, SWT.NULL);
		lName.setText("Name:");
		lName
				.setToolTipText("Enter the name of the series or some other unique identifier here");

		tName = new Text(gRB, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		tName.setLayoutData(gd);
		tName
				.setToolTipText("Enter the name of the series or some other unique identifier here");

		// Release Group
		Label lGroup = new Label(gRB, SWT.NULL);
		lGroup.setText("Group:");
		lGroup
				.setToolTipText("Enter the release group's name or some other unique identifier here");

		tGroup = new Text(gRB, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		tGroup.setLayoutData(gd);
		tGroup
				.setToolTipText("Enter the release group's name or some other unique identifier here");

		Label lDeny = new Label(gRB, SWT.NULL);
		lDeny.setText("Exclude:");
		lDeny.setToolTipText("Enter words to exclude, seperate by ;");

		final Text tDeny = new Text(gRB, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		tDeny.setLayoutData(gd);
		tDeny.setToolTipText("Enter words to exclude, seperate by ;");

		final Button dontEscape = new Button(gRB, SWT.CHECK);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		dontEscape.setLayoutData(gd);
		dontEscape.setText("Don't escape special chars");
		dontEscape
				.setToolTipText("Don't escape special Regex chars like | ( [ and so on.");

		// Build Rule button
		Button bBuild = new Button(gRB, SWT.PUSH);
		bBuild.setText("Build Rule");
		bBuild.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				String rule = ".*?";
				String name = dontEscape.getSelection() ? tName.getText()
						.trim() : escapeRegexSpecialChars(tName.getText()
						.trim());
				// manipulate the name some
				name = name.replaceAll(" ", ".+?");
				String group = dontEscape.getSelection() ? tGroup.getText()
						.trim() : escapeRegexSpecialChars(tGroup.getText()
						.trim());
				// manipulate the group some
				group = group.replaceAll(" ", ".+?");
				if (name.length() >= 1 && group.length() >= 1) {
					// Example: .*?Nanashi.*?Eureka.*?|.*?Eureka.*?Nanashi.*?
					rule = ".*?" + group + ".*?" + name + ".*?|.*?" + name
							+ ".*?" + group + ".*?";
				} else if (name.length() >= 1 || group.length() > 1) {
					if (name.length() > 1) {
						rule += name + ".*?";
					} else {
						rule += group + ".*?";
					}
				}
				patternText.setText(rule);

				String deny = tDeny.getText();
				if (!deny.equals("")) {
					String[] denyWords = deny.split(";");
					deny = ".*?"
							+ (dontEscape.getSelection() ? denyWords[0].trim()
									: escapeRegexSpecialChars(denyWords[0]
											.trim())).replaceAll(" ", ".+?")
							+ ".*?";
					for (int i = 1; i < denyWords.length; i++) {
						deny += "|.*?"
								+ (dontEscape.getSelection() ? denyWords[i]
										.trim()
										: escapeRegexSpecialChars(denyWords[i]
												.trim()))
										.replaceAll(" ", ".+?") + ".*?";
					}
					denyText.setText(deny);
				}
			}
		});
		gd = new GridData();
		gd.horizontalSpan = 2;
		bBuild.setLayoutData(gd);

		Label fileNameLabel = new Label(gRB, SWT.None);
		fileNameLabel.setText("Parse from this:");

		fileNameText = new Text(gRB, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fileNameText.setLayoutData(gd);
		fileNameText.setToolTipText("Put a filename in here, and click parse.");

		Button parseFileName = new Button(gRB, SWT.PUSH);
		parseFileName.setText("Parse!");
		parseFileName.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				tName.setText(suggestName(fileNameText.getText()));
				tGroup.setText(suggestGroup(fileNameText.getText()));
			}
		});

		Button parseFilePath = new Button(gRB, SWT.PUSH);
		parseFilePath.setText("Parse Filename!");
		parseFilePath.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				FileDialog fd = new FileDialog(shell, SWT.OPEN);
				String fileName = fd.open();
				if (fileName != null) {
					int index = fileName.lastIndexOf('/');
					index = index != -1 ? index : fileName.lastIndexOf('\\');
					if (index != -1) {
						fileName = fileName.substring(index + 1);
					}
					tName.setText(suggestName(fileName));
					tGroup.setText(suggestGroup(fileName));
				}
			}
		});

		// RegEx help group
		Group gHelp = new Group(rComp, SWT.NULL);
		gHelp.setText("RegEx Quick Help Guide");
		gHelp.setLayout(new GridLayout(1, false));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gHelp.setLayoutData(gd);

		// labels for help
		Label help1 = new Label(gHelp, SWT.NULL);
		help1.setText(".*?foo.*? = Match single term foo");

		Label help2 = new Label(gHelp, SWT.NULL);
		help2.setText(".*?foo.*?bar.*? = Match both foo and bar");

		Label help3 = new Label(gHelp, SWT.NULL);
		help3.setText(".*?foo.*?bar.*?|.*?bar.*?foo.*?");

		Label help4 = new Label(gHelp, SWT.NULL);
		help4.setText("  = Will match both foo and bar in any order");

		// button comp
		Composite cBut = new Composite(rComp, SWT.NULL);
		cBut.setLayout(new GridLayout(1, false));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		cBut.setLayoutData(gd);

		// Save rule
		bSave = new Button(cBut, SWT.PUSH);
		bSave.setText("Insert Patterns into Current Filter and Close");
		bSave.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				gui.getFilterView().setPatternText(null, patternText.getText(),
						denyText.getText());
				shell.close();
			}
		});

		bCreate = new Button(cBut, SWT.PUSH);
		bCreate.setText("Create New Filter and Close");
		bCreate.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				gui.getFilterView().clearFilterControls();
				gui.getFilterView().setPatternText(tName.getText(),
						patternText.getText(), denyText.getText());
				shell.close();
			}
		});

		// Close button
		Button bClose = new Button(cBut, SWT.PUSH);
		bClose.setText("Cancel");
		bClose.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				shell.close();
			}
		});

		// open the shell
		centerShellandOpen(shell);
	}

	public static String urlProcess (String urlString) {
		try {
			urlString = URLDecoder.decode(urlString, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		if (urlString.contains("/")) {
			urlString = urlString.substring(urlString.lastIndexOf('/') + 1);
		}

		urlString = urlString.replace('_', ' ');

		urlString = urlString.replaceAll("\\[.*?\\]|\\d", "");

		if (urlString.contains(".")) {
			urlString = urlString.substring(0, urlString.indexOf('.'));
		}
		urlString = urlString.trim();
		return urlString;
	}

	/**
	 * Centers a Shell and opens it relative to the users Monitor
	 * 
	 * @param shell
	 */

	private void centerShellandOpen (Shell shell) {
		// open shell
		shell.pack();

		// Center Shell
		Monitor primary = shell.getDisplay().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell.setLocation(x, y);

		// open shell
		shell.open();
	}

	/**
	 * tests the input from the GUI This is to be run by both the button
	 * listener and a CR listener on the text box
	 * 
	 */
	private void testInput () {
		display.asyncExec(new SWTSafeRunnable() {

			@Override
			public void runSafe () {
				final String pattern = patternText.getText();
				final String denyPattern = denyText.getText();
				if (pattern == null || pattern.equalsIgnoreCase("")
						|| pattern.length() < 1) {
					MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION
							| SWT.OK);
					mb.setText("No input given");
					mb
							.setMessage("Please input a RegEx rule to test in the input field.");
					mb.open();
					return;
				}

				if (matchesTable.getItemCount() > 0) {
					matchesTable.removeAll();
				}

				// cycle throught the feeds
				for (RSSFeed feed : feeds) {
					List<RSSItem> list = feed.getItems();
					Filter f = new Filter("Test", pattern, false, false);
					f.setDenyPattern(denyPattern);
					for (RSSItem rssItem : list) {
						try {
							// see if there is a match
							if (f.checkOnly(rssItem)) {
								TableItem item = new TableItem(matchesTable,
										SWT.NULL);

								item.setText(0, rssItem.getTitle());
								item.setText(1, rssItem.getLink());
							}

						} catch (Exception e) {
							patternText.setText(patternText.getText()
									+ " (INVALID RULE!)");
							return;
						}

					}
				}

			}

		});
	}

	public static String suggestName (String s) {
		try {
			s = URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if (s.contains("/")) {
			s = s.substring(s.lastIndexOf('/') + 1);
		}
		s = s.replace('_', ' ');
		s = s.replaceAll("\\[.*?\\]|\\d", "");
		if (s.contains(".")) {
			s = s.substring(0, s.indexOf('.'));
		}
		return s.trim();
	}

	public static String suggestGroup (String s) {
		try {
			s = URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if (s.contains("/")) {
			s = s.substring(s.lastIndexOf('/') + 1);
		}
		s = s.replace('_', ' ');
		s = s.replaceAll("\\[[A-Fa-f0-9]{8}\\]|\\d", "");
		Matcher m = groupPattern.matcher(s);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	public String escapeRegexSpecialChars (String s) {
		return s.replaceAll(
				"(\\(|\\||\\)|\\[|\\]|\\?|\\+|\\*|\\$|\\^|\\\\|\\.)", "\\\\$1");
	}

	private void filterItems (String text) {
		text = text.toLowerCase();
		filteredItems.clear();
		for (RSSItem i : items) {
			if (i.getTitle().toLowerCase().contains(text)) {
				filteredItems.add(i);
			}
		}
	}

	/**
	 * Static open method
	 */
	public static void open (GUI gui, String currentPattern,
			String currentDenyPattern, Display display, RSSFeed[] feeds) {
		if (instance == null || instance.shell == null
				|| instance.shell.isDisposed()) {
			new GUIPatternAssistant(gui, currentPattern, currentDenyPattern,
					display, feeds);
		} else {
			instance.shell.setActive();
		}
	}

	public static void open (GUI gui, RSSItem src, Display display,
			RSSFeed[] feeds) {
		if (instance == null || instance.shell == null
				|| instance.shell.isDisposed()) {
			new GUIPatternAssistant(gui, src, display, feeds);
		} else {
			instance.shell.setActive();
		}
	}
}
