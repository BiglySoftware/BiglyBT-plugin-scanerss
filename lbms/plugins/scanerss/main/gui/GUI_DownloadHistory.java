/**
 *
 */
package lbms.plugins.scanerss.main.gui;

import java.util.List;

import lbms.plugins.scanerss.main.DownloadHistory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Damokles
 * 
 */
public class GUI_DownloadHistory {
	private Display	display;
	private Shell	shell;

	/**
	 *
	 */
	public GUI_DownloadHistory(Display _display, DownloadHistory dh) {
		this.display = _display;

		final List<List<Integer>> seasonList = dh.getSeasonList();
		boolean dataFound = false;

		// the main shell
		shell = new Shell(display, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setLayout(new GridLayout(1, false));
		shell.setText("DownloadHistory");

		// the parent composite
		final Composite parent = new Composite(shell, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));

		GridData gd = new GridData(GridData.FILL_BOTH);
		parent.setLayoutData(gd);

		for (int i = 1; i < seasonList.size(); i++) {
			List<Integer> season = seasonList.get(i);
			if (season == null) {
				continue;
			}
			dataFound = true;
			final Group seasonGroup = new Group(parent, SWT.NONE);
			gd = new GridData(GridData.FILL_BOTH);
			seasonGroup.setLayoutData(gd);

			seasonGroup.setText("Season " + i);
			final int seasonID = i;

			Menu contextMenu = new Menu(seasonGroup);
			MenuItem delete = new MenuItem(contextMenu, SWT.PUSH);
			delete.setText("Delete Season " + 1);
			delete.addSelectionListener(new SelectionAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org
				 * .eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected (SelectionEvent e) {
					seasonList.set(seasonID, null);
					seasonGroup.dispose();
					parent.pack();
				}
			});

			seasonGroup.setMenu(contextMenu);

			seasonGroup.setLayout(new GridLayout(11, false));

			Label indexLabel0 = new Label(seasonGroup, SWT.CENTER);
			indexLabel0.setText("");

			for (int index = 0; index <= 9; index++) {
				Label indexLabel = new Label(seasonGroup, SWT.CENTER);
				indexLabel.setText(Integer.toString(index));
			}

			Label sLabel0 = new Label(seasonGroup, SWT.CENTER);
			sLabel0.setText("0");

			Label sLabel = new Label(seasonGroup, SWT.CENTER);
			sLabel.setText("");

			for (int e = 1; e < season.size(); e++) {
				if (e % 10 == 0) {
					Label indexLabel = new Label(seasonGroup, SWT.RIGHT);
					indexLabel.setText(Integer.toString(e / 10));
				}
				Button episode = new Button(seasonGroup, SWT.CHECK);
				episode.setSelection(season.get(e) > 0);
				episode.addSelectionListener(new DHListener(season, e));
				episode.setToolTipText("Season: " + i + " Episode: " + e);
			}
		}

		if (!dataFound) {
			Label notice = new Label(parent, SWT.NONE);
			notice
					.setText("No DownloadHistory Data was found.\nIt appears that this filter never Matched anything.");
		}

		Button close = new Button(parent, SWT.PUSH);
		close.setText("Close");
		close.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				shell.close();
			}
		});
		shell.layout();
		centerShellandOpen(shell);
	}

	public static void open (final Display display, final DownloadHistory dh) {
		display.asyncExec(new SWTSafeRunnable() {
			@Override
			public void runSafe () {
				new GUI_DownloadHistory(display, dh);
			}
		});
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

	private static class DHListener implements SelectionListener {
		List<Integer>	season;
		int				episode;

		public DHListener(List<Integer> s, int e) {
			season = s;
			episode = e;
		}

		public void widgetDefaultSelected (SelectionEvent e) {
		}

		public void widgetSelected (SelectionEvent e) {
			season.set(episode, ((Button) e.widget).getSelection() ? 1 : 0);
		}
	}

}
