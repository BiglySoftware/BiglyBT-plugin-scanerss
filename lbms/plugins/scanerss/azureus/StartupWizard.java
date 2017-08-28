/**
 * First time startup wizard.. should only run once and allow the user to set up
 * their settings
 * 
 * 
 */

package lbms.plugins.scanerss.azureus;

import lbms.plugins.scanerss.main.ImageRepository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.biglybt.pif.PluginConfig;

public class StartupWizard {

	//Global Display
	private static Display			display;

	//Main Shell for wizard
	private Shell					shell;

	//Sash for shell
	private SashForm				sash;

	//Font for shell
	Font							font16;

	//Main composite for Right and Left hand sides
	private Composite				lComp;
	private Composite				rComp;
	private Composite				parent;

	//Labels for steps
	private Label					step1, step2, step3;

	//Step we are on
	int								step		= 1;

	//Buttons for bComp
	private Button					btnPrevious, btnContinue;

	//Stored Info
	private boolean					useStats	= true;
	private String					dirString;

	//instance to make sure no more than one of these is open
	private static StartupWizard	instance;

	/**
	 * Main open
	 * 
	 * @param _display
	 */
	private StartupWizard(Display _display) {
		display = _display;
		instance = this;
		shell = new Shell(display /* SWT.APPLICATION_MODAL */);
		shell.setLayout(new GridLayout(1, false));
		shell.setText("AzSMRC Startup Wizard");

		//Shell listener to make sure we dispose of everything
		shell.addDisposeListener(new DisposeListener() {

			public void widgetDisposed (DisposeEvent arg0) {
				if (font16 != null && !font16.isDisposed()) {
					font16.dispose();
				}
			}

		});

		//put sash on shell
		sash = new SashForm(shell, SWT.HORIZONTAL);
		sash.setLayout(new GridLayout());

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 300;
		gridData.widthHint = 750;
		sash.setLayoutData(gridData);

		//Left hand list side
		lComp = new Composite(sash, SWT.BORDER);
		lComp.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		lComp.setLayout(gridLayout);

		gridData = new GridData(GridData.FILL_BOTH);
		lComp.setLayoutData(gridData);

		//Labels for lComp
		step1 = new Label(lComp, SWT.LEFT);
		step1.setText("Step 1");
		step1.setBackground(display.getSystemColor(SWT.COLOR_GRAY));

		step2 = new Label(lComp, SWT.LEFT);
		step2.setText("Step 2");
		step2.setBackground(display.getSystemColor(SWT.COLOR_WHITE));

		step3 = new Label(lComp, SWT.LEFT);
		step3.setText("Step 3");
		step3.setBackground(display.getSystemColor(SWT.COLOR_WHITE));

		//Right hand composite
		rComp = new Composite(sash, SWT.NULL);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		rComp.setLayout(gridLayout);

		gridData = new GridData(GridData.FILL_BOTH);
		rComp.setLayoutData(gridData);

		//Main Parent Composite to draw on for right side
		parent = new Composite(rComp, SWT.BORDER);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		parent.setLayout(gridLayout);

		gridData = new GridData(GridData.FILL_BOTH);
		parent.setLayoutData(gridData);

		//Button Composite
		Composite bComp = new Composite(rComp, SWT.NULL);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		bComp.setLayout(gridLayout);

		gridData = new GridData(GridData.FILL_HORIZONTAL);
		bComp.setLayoutData(gridData);

		btnPrevious = new Button(bComp, SWT.PUSH);
		btnPrevious.setText("Previous");
		btnPrevious.setEnabled(false);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gridData.grabExcessHorizontalSpace = true;
		btnPrevious.setLayoutData(gridData);
		btnPrevious.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (step != 0) {
					loadStep(--step);
				}
			}
		});

		btnContinue = new Button(bComp, SWT.PUSH);
		btnContinue.setText("Continue");
		btnContinue.setEnabled(true);
		btnContinue.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (step != 3) {
					loadStep(++step);
				} else {
					//This is where we need to commit everything
					//TODO
					try {
						PluginConfig pc = ScanerssAzPlugin
								.getPluginInterface().getPluginconfig();
						pc.setPluginParameter("scanerss.statistics.allow",
								useStats);

						if (dirString != null && !dirString.equals("")) {
							pc.setUnsafeStringParameter("Default save path",
									dirString);
						}
						//Save all
						pc.save();
					} catch (Exception e) {
					}
					shell.dispose();
				}

			}
		});

		sash.setWeights(new int[] { 10, 90 });
		//open up the first step
		step = 1;
		loadStep(step);

		//open shell
		centerShellandOpen(shell);

	}

	/**
	 * Step 1
	 */
	private void step1 () {
		Control[] controls = parent.getChildren();
		for (Control control : controls) {
			control.dispose();
		}
		btnPrevious.setEnabled(false);
		btnContinue.setText("Continue");

		//Welcome stuff here
		Label welcome1 = new Label(parent, SWT.CENTER);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		welcome1.setLayoutData(gridData);

		welcome1.setText("Welcome to ScaneRSS");
		Font initialFont = welcome1.getFont();
		FontData[] fontData = initialFont.getFontData();
		for (int i = 0; i < fontData.length; i++) {
			fontData[i].setHeight(16);
		}
		font16 = new Font(display, fontData);
		welcome1.setFont(font16);

		Label welcome2 = new Label(parent, SWT.CENTER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		welcome2.setLayoutData(gridData);
		welcome2.setText("\n\nThis wizard will guide you\n"
				+ "through setting up a few basic items for ScaneRSS\n"
				+ "Please take the time to complete this wizard");

		parent.layout();
	}

	/**
	 * Step 2
	 */
	private void step2 () {
		Control[] controls = parent.getChildren();
		for (Control control : controls) {
			control.dispose();
		}
		btnPrevious.setEnabled(true);
		btnContinue.setText("Finish");

		Composite comp = new Composite(parent, SWT.NULL);
		comp.setLayout(new GridLayout(2, false));

		GridData gd = new GridData(GridData.FILL_BOTH);
		comp.setLayoutData(gd);

		final Label labelDir = new Label(comp, SWT.NULL);
		labelDir.setText("Default Save Directory");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		labelDir.setLayoutData(gd);

		final Text dir = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		dir.setLayoutData(gd);
		if (dirString != null) {
			dir.setText(dirString);
		} else {
			dir.setText(ScanerssAzPlugin.getPluginInterface().getPluginconfig()
					.getUnsafeStringParameter("Default save path"));
		}
		Label dirImg = new Label(comp, SWT.NULL);
		dirImg.setImage(ImageRepository.getImage("fileopen"));
		dirImg.addMouseListener(new MouseListener() {

			public void mouseDoubleClick (MouseEvent arg0) {
			}

			public void mouseDown (MouseEvent arg0) {
				DirectoryDialog dialog = new DirectoryDialog(shell);
				String results = dialog.open();
				if (results != null) {
					dirString = results;
					dir.setText(results);
				}
			}

			public void mouseUp (MouseEvent arg0) {
			}

		});

		//Label explaining everything
		Label label = new Label(comp, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		label
				.setText("\n\nFor ScaneRSS to function properly, the default save directory for Azureus must "
						+ "be specified");

		parent.layout();
	}

	/**
	 * Step 3 Stats
	 */
	private void step3 () {
		Control[] controls = parent.getChildren();
		for (Control control : controls) {
			control.dispose();
		}
		btnPrevious.setEnabled(true);
		btnContinue.setText("Continue");

		Composite comp = new Composite(parent, SWT.NULL);
		comp.setLayout(new GridLayout(1, false));

		GridData gd = new GridData(GridData.FILL_BOTH);
		comp.setLayoutData(gd);

		final Button btnUseStats = new Button(comp, SWT.CHECK);
		btnUseStats.setText("Allow Anonymous Statistics");
		btnUseStats.setSelection(useStats);
		btnUseStats.addListener(SWT.Selection, new Listener() {

			public void handleEvent (Event arg0) {
				useStats = btnUseStats.getSelection();

			}

		});
		//Label explaining everything
		Label label = new Label(comp, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		label
				.setText("\n\nAllow ScaneRSS to send version and random ID, when checking for updates, for anonymous usage statistics."
						+ "\n - The data is stored for 24 hours and will be deleted afterwards");

		parent.layout();
	}

	/**
	 * Loads a step of given integer
	 */
	private void loadStep (int stepToLoad) {
		switch (stepToLoad) {
		case 1:
			step1.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
			step2.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			step3.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			step1();
			break;
		case 2:
			step1.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			step2.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
			step3.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			step2();
			break;
		case 3:
			step1.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			step2.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			step3.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
			step3();
			break;

		}
	}

	/**
	 * Centers a Shell and opens it relative to the users Monitor
	 * 
	 * @param shell
	 */

	public static void centerShellandOpen (Shell shell) {
		//open shell
		shell.pack();

		//Center Shell
		Monitor primary = display.getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell.setLocation(x, y);

		//open shell
		shell.open();
	}

	/**
	 * Static open method
	 */
	public static void open (Display display) {
		if (display == null) {
			return;
		}
		if (instance == null || instance.shell == null
				|| instance.shell.isDisposed()) {
			new StartupWizard(display);
		} else {
			instance.shell.setActive();
		}
	}
}