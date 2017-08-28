package lbms.tools.i18n.swt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class NewDialog {
	private String		sTitleKey;

	private String[]	textValue			= { "", "" };

	private Pattern		placeHolderPattern	= Pattern.compile("\\{(\\d+)\\}");

	public NewDialog(String Title) {
		this.sTitleKey = Title;

		this.setTextValue("", "");
	}

	public String[] open() {
		final Display display = ETC.getETC().getDisplay();
		if (display == null) {
			return null;
		}

		final Shell shell = new Shell(display.getActiveShell());
		shell.setText(sTitleKey);

		GridLayout layout = new GridLayout();
		shell.setLayout(layout);

		Label label = new Label(shell, SWT.WRAP);
		label.setText("New Key Name");

		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = 200;
		label.setLayoutData(gridData);

		final Text textKey = new Text(shell, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 300;
		textKey.setLayoutData(gridData);
		textKey.setText(textValue[0]);
		textKey.selectAll();

		try {
			if (textKey.getText().lastIndexOf(".") > 0) {
				textKey.setSelection(textKey.getText().lastIndexOf(".") + 1,
						textKey.getText().length());
			}
		} catch (Exception e) {
		}

		Label label2 = new Label(shell, SWT.WRAP);
		label2.setText("Default Value");

		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = 200;
		label.setLayoutData(gridData);

		final StyledText textObj = new StyledText(shell, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 300;
		gridData.heightHint = 400;
		textObj.setLayoutData(gridData);
		textObj.setText(textValue[1]);
		textObj.selectAll();
		textObj.addModifyListener(new ModifyListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
			public void modifyText(ModifyEvent e) {
				String text = textObj.getText();
				StyleRange unstyled = new StyleRange();
				unstyled.start = 0;
				unstyled.length = text.length();
				textObj.setStyleRange(unstyled);
				Matcher m = placeHolderPattern.matcher(text);
				while (m.find()) {
					int number = Integer.parseInt(m.group(1));
					String find = "{" + number + "}";
					int i = text.indexOf(find);
					if (i > -1) {
						StyleRange sr = new StyleRange(i, find.length(), null,
								null, SWT.BOLD);
						textObj.setStyleRange(sr);
					}
				}

			}
		});

		Composite panel = new Composite(shell, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 3;
		panel.setLayout(layout);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		panel.setLayoutData(gridData);
		Button ok = new Button(panel, SWT.PUSH);
		ok.setText("OK");
		gridData = new GridData();
		gridData.widthHint = 70;
		ok.setLayoutData(gridData);
		shell.setDefaultButton(ok);
		ok.addListener(SWT.Selection, new Listener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			 */
			public void handleEvent(Event event) {
				try {
					setTextValue(textKey.getText(), textObj.getText());
					shell.dispose();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		Button cancel = new Button(panel, SWT.PUSH);
		cancel.setText("Cancel");
		gridData = new GridData();
		gridData.widthHint = 70;
		cancel.setLayoutData(gridData);
		cancel.addListener(SWT.Selection, new Listener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			 */
			public void handleEvent(Event event) {
				shell.dispose();
			}
		});

		shell.pack();
		centerShellRelativeToandOpen(shell, ETC.getETC().getMainWindow()
				.getShell());
		setTextValue(null, null);
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		return getTextValue();
	}

	/**
	 * @param textValue The textValue to set.
	 */
	public void setTextValue(String keyValue, String defaultValue) {
		this.textValue[0] = keyValue;
		this.textValue[1] = (defaultValue != null) ? defaultValue.replace(
				"\\n", "\n") : defaultValue;
	}

	/**
	 * @return Returns the textValue.
	 */
	public String[] getTextValue() {
		return textValue;
	}

	/**
	 * Centers a Shell and opens it relative to given control
	 * 
	 * @param shell
	 * @param control
	 */

	public static void centerShellRelativeToandOpen(final Shell shell,
			final Control control) {
		// open shell
		shell.pack();

		// Center Shell

		final Rectangle bounds = control.getBounds();
		final Point shellSize = shell.getSize();
		shell.setLocation(bounds.x + (bounds.width / 2) - shellSize.x / 2,
				bounds.y + (bounds.height / 2) - shellSize.y / 2);

		// open shell
		shell.open();
	}
}
