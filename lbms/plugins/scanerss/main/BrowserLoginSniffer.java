package lbms.plugins.scanerss.main;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lbms.plugins.scanerss.main.gui.SWTSafeRunnable;
import lbms.tools.Download;
import lbms.tools.DownloadListener;
import lbms.tools.HTTPDownload;
import lbms.tools.HTTPDownload.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Damokles
 * 
 */
public class BrowserLoginSniffer {

	private Pattern				adPattern		= Pattern
														.compile(
																"(ads|adBlock|adColumn|ad\\d|adContainer|adbar|falkag)",
																Pattern.CASE_INSENSITIVE);
	private Pattern				headPattern		= Pattern
														.compile(
																"(<head>)",
																Pattern.CASE_INSENSITIVE);
	private Pattern				htmlPattern		= Pattern
														.compile(
																"(<html>)",
																Pattern.CASE_INSENSITIVE);
	private Pattern				formPOST2GET	= Pattern
														.compile(
																"<form(.*)method=(?:'|\")?post(?:'|\")?(.*)>",
																Pattern.CASE_INSENSITIVE);

	private Display				display;
	private Shell				shell;
	private Text				locationbar;
	private Button				btnGO;
	private Browser				browser;
	private Label				statusBar;
	private Text				debugText;
	private HTTPDownload		dl;

	private String				cookie;
	private String				loginUser, loginPass;
	private String				referer;

	private DownloadListener	dlListener		= new DownloadListener() {
													/*
													 * (non-Javadoc)
													 * 
													 * @see
													 * lbms.tools.DownloadListener
													 * #
													 * debugMsg(java.lang.String
													 * )
													 */
													public void debugMsg (
															String msg) {
														System.out.println(msg);
													}

													/*
													 * (non-Javadoc)
													 * 
													 * @see
													 * lbms.tools.DownloadListener
													 * #progress(long, long)
													 */
													public void progress (
															long bytesRead,
															long bytesTotal) {
														// TODO Auto-generated method stub

													}

													/*
													 * (non-Javadoc)
													 * 
													 * @see
													 * lbms.tools.DownloadListener
													 * #stateChanged(int, int)
													 */
													public void stateChanged (
															final int oldState,
															final int newState) {
														display
																.asyncExec(new SWTSafeRunnable() {
																	/*
																	 * (non-Javadoc
																	 * )
																	 * 
																	 * @see
																	 * java.
																	 * lang.
																	 * Runnable
																	 * #run()
																	 */
																	@Override
																	public void runSafe () {
																		if (newState == Download.STATE_FINISHED) {
																			try {
																				String contentHTML = parse(new String(
																						dl
																								.getBuffer()
																								.toByteArray(),
																						"UTF8"));
																				if (dl
																						.getHeaderFields() != null) {
																					Map<String, List<String>> headerFields = dl
																							.getHeaderFields();
																					dumpHeaders(headerFields);
																					debugText
																							.setText(dumpHeadersToString(headerFields)
																									+ "\n\n"
																									+ contentHTML);
																					if (headerFields
																							.containsKey("Set-Cookie")) {
																						cookie = "";
																						for (String c : headerFields
																								.get("Set-Cookie")) {
																							if (c
																									.contains(";")) {
																								cookie += c
																										.substring(
																												0,
																												c
																														.indexOf(';'))
																										+ "; ";
																							} else {
																								cookie += c
																										+ "; ";
																							}
																						}
																					}
																				} else {
																					debugText
																							.setText(contentHTML);
																				}
																				referer = locationbar
																						.getText(); //safe referer
																				locationbar
																						.setText(dl
																								.getSource()
																								.toExternalForm());
																				browser
																						.setText(contentHTML);
																			} catch (UnsupportedEncodingException e) {
																				e
																						.printStackTrace();
																			}
																		} else if (newState == Download.STATE_FAILURE) {
																			if (dl
																					.getHeaderFields() != null) {
																				Map<String, List<String>> headerFields = dl
																						.getHeaderFields();
																				dumpHeaders(headerFields);
																				debugText
																						.setText(dumpHeadersToString(headerFields));
																				if (headerFields
																						.containsKey("Set-Cookie")) {
																					cookie = "";
																					for (String c : headerFields
																							.get("Set-Cookie")) {
																						if (c
																								.contains(";")) {
																							cookie += c
																									.substring(
																											0,
																											c
																													.indexOf(';'))
																									+ "; ";
																						} else {
																							cookie += c
																									+ "; ";
																						}
																					}
																				}
																			}
																			if (dl
																					.getResponeCode() == 401) { //401 Unauthorized
																				System.out
																						.println("401 Unauthorized");
																				askForPassword();
																			}
																		}
																	}
																});
													}
												};

	public void createContents () {
		shell = new Shell(display);
		shell.setLayout(new GridLayout(2, false));

		locationbar = new Text(shell, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		locationbar.setLayoutData(gd);
		locationbar.setText("http://www.google.de");

		btnGO = new Button(shell, SWT.PUSH);
		btnGO.setText("Go To");
		btnGO.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				try {
					URL url = new URL(locationbar.getText());
					System.out.println(locationbar.getText());
					gotoURL(url);
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				}
			}
		});

		browser = new Browser(shell, SWT.NONE);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 800;
		gd.heightHint = 600;
		gd.horizontalSpan = 2;
		browser.setLayoutData(gd);

		statusBar = new Label(shell, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		statusBar.setLayoutData(gd);
		statusBar.setText("Ready To Go!");

		debugText = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 800;
		gd.heightHint = 200;
		gd.horizontalSpan = 2;
		debugText.setLayoutData(gd);

		browser.addLocationListener(new LocationAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.browser.LocationAdapter#changing(org.eclipse.
			 * swt.browser.LocationEvent)
			 */
			@Override
			public void changing (LocationEvent event) {
				System.out.println(event.location);
				if (event.location.equalsIgnoreCase("about:blank")) {
					return;
				}
				event.doit = false; //cancel Browser intern changing
				Matcher m = adPattern.matcher(event.location);
				if (m.find()) {
					return; //filter ads since they screw up the browser
				}
				try {
					URL url = new URL(event.location);
					gotoURL(url);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		});
		browser.addStatusTextListener(new StatusTextListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.browser.StatusTextListener#changed(org.eclipse
			 * .swt.browser.StatusTextEvent)
			 */
			public void changed (StatusTextEvent event) {
				statusBar.setText(event.text);
			}
		});
		browser
				.setText("<html><body>This is the interactive Login capture browser of ScaneRSS.<br>Note Back is not available.</body></html>");
		shell.open();
	}

	/**
	 * Just use this as Debug
	 */
	public void run () {
		display = new Display();
		createContents();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	private String parse (String input) {
		Matcher m = headPattern.matcher(input);
		if (m.find()) {
			input = m.replaceFirst("$1<base href=\""
					+ dl.getSource().toExternalForm() + "\">");
		} else {
			m = htmlPattern.matcher(input);
			input = m.replaceFirst("$1<head><base href=\""
					+ dl.getSource().toExternalForm() + "\"></head>");
		}
		m = formPOST2GET.matcher(input);
		input = m
				.replaceAll("<form $1 method=\"GET\" $2><input type=\"hidden\" name=\"_ScaneRSS_FORMSNIFFER_\" value=\"POST\"");
		return input;
	}

	private void gotoURL (URL url) {
		if (url.getQuery() != null
				&& url.getQuery().contains("_ScaneRSS_FORMSNIFFER_")) {
			String tmp = url.getQuery().replace("_ScaneRSS_FORMSNIFFER_=POST&",
					""); //remove tag that indicates that a POST was converted to get
			try {
				url = new URL(url.toExternalForm().substring(0,
						url.toExternalForm().indexOf('?'))); //remove the query
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			dl = new HTTPDownload(url);
			dl.setMethod(Method.POST);
			dl.setPostData(tmp);
		} else {
			dl = new HTTPDownload(url);
		}
		if (cookie != null) {
			dl.setCookie(cookie);
		}
		if (referer != null) {
			dl.setReferer(referer);
		}
		if (loginUser != null && loginPass != null) {
			dl.setLogin(loginUser, loginPass);
		}
		dl.addDownloadListener(dlListener);
		Thread t = new Thread(dl);
		t.setDaemon(true);
		t.start();
	}

	public static void main (String[] args) {
		/*
		 * Relative links: use the HTML base tag <html><head> <base
		 * href="http://www.eclipse.org/swt/" > <title>HTML Test</title></head>
		 * <body><a href="faq.php">local link</a></body></html>;
		 */

		new BrowserLoginSniffer().run();
	}

	private void dumpHeaders (Map<String, List<String>> headerFields) {
		for (Entry<String, List<String>> entry : headerFields.entrySet()) {
			System.out.print(entry.getKey() + ": ");
			for (String value : entry.getValue()) {
				System.out.println("\t" + value);
			}
		}
	}

	private String dumpHeadersToString (Map<String, List<String>> headerFields) {
		StringBuffer sb = new StringBuffer();
		for (Entry<String, List<String>> entry : headerFields.entrySet()) {
			sb.append(entry.getKey() + ": ");
			for (String value : entry.getValue()) {
				sb.append("\t" + value + "\n");
			}
		}
		return sb.toString();
	}

	private void askForPassword () {
		final Shell pwShell = new Shell(display, SWT.APPLICATION_MODAL);
		pwShell.setLayout(new GridLayout(2, false));
		Label uLable = new Label(pwShell, SWT.NONE);
		uLable.setText("Username:");

		final Text uName = new Text(pwShell, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		uName.setLayoutData(gd);

		Label pLable = new Label(pwShell, SWT.NONE);
		pLable.setText("Password:");

		final Text pw = new Text(pwShell, SWT.PASSWORD | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		pw.setLayoutData(gd);

		Button ok = new Button(pwShell, SWT.PUSH);
		ok.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected (SelectionEvent e) {
				loginUser = uName.getText();
				loginPass = pw.getText();
				gotoURL(dl.getSource());
				pwShell.close();
			}
		});
		ok.setText("Login");

		Button abort = new Button(pwShell, SWT.PUSH);
		abort.setText("Abort");
		abort.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected (SelectionEvent e) {
				pwShell.close();
			}
		});
		pwShell.pack();
		pwShell.open();
	}
}
