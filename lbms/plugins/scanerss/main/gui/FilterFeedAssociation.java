/**
 * 
 */
package lbms.plugins.scanerss.main.gui;

import java.util.Set;

import lbms.plugins.scanerss.main.Filter;
import lbms.plugins.scanerss.main.RSSFeed;
import lbms.plugins.scanerss.main.RSSFeedManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Leonard
 * 
 */
public class FilterFeedAssociation {
	private Display	display;
	private Shell	shell;

	public FilterFeedAssociation(Display _display, final Filter filter,
			final RSSFeedManager manager) {
		this.display = _display;

		// the main shell
		shell = new Shell(display, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setLayout(new GridLayout(1, false));
		shell.setText("Feed Association");

		// the parent composite
		GridData gridData = new GridData(GridData.FILL_BOTH);
		Composite parent = new Composite(shell, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));
		parent.setLayoutData(gridData);

		final RSSFeed[] feeds = manager.getFeedsAsArray();

		int height = 150 + feeds.length * 20;
		Rectangle monitor = display.getPrimaryMonitor().getBounds();
		int maxheight = monitor.height - 100;
		Point cursor = display.getCursorLocation();
		height = height < maxheight - cursor.y ? height : maxheight - cursor.y;

		shell.setLocation(cursor.x + 400 > monitor.width ? monitor.width - 400
				: cursor.x - 40, cursor.y);
		shell.setMinimumSize(400, height);

		Table feedTable = new Table(parent, SWT.CHECK | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.SINGLE | SWT.VIRTUAL);

		gridData = new GridData(GridData.FILL_BOTH);
		feedTable.setLayoutData(gridData);
		feedTable.setHeaderVisible(false);

		feedTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent (Event event) {
				TableItem item = (TableItem) event.item;
				item.setText(feeds[event.index].getName());
				item.setData("feed", feeds[event.index]);
				if (manager.getLinks().containsKey(feeds[event.index].getId())) {
					Set<Filter> x = manager.getLinks().get(
							feeds[event.index].getId());
					if (x.contains(filter)) {
						item.setChecked(true);
					} else {
						item.setChecked(false);
					}

				} else {
					item.setChecked(false);
				}
			}
		});

		feedTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				if (event.detail == SWT.CHECK) {
					TableItem item = (TableItem) event.item;
					RSSFeed feed = (RSSFeed) item.getData("feed");
					if (item.getChecked()) {
						manager.assignFilterToFeed(filter, feed);
					} else {
						manager.removeFilterFromFeed(filter, feed);
					}
				}
			}
		});

		feedTable.setItemCount(feeds.length);

		Button close = new Button(parent, SWT.PUSH);
		close.setText("Close");
		close.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				shell.close();
			}
		});

		shell.pack();
		shell.open();
	}

}
