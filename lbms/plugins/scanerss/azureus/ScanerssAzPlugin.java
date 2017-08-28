package lbms.plugins.scanerss.azureus;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessControlException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import lbms.plugins.scanerss.main.Episode;
import lbms.plugins.scanerss.main.Filter;
import lbms.plugins.scanerss.main.RSSFeedManager;
import lbms.tools.CryptoTools;

import org.eclipse.swt.widgets.Display;
import org.jdom.Element;

import com.biglybt.core.util.*;
import com.biglybt.core.category.*;
import com.biglybt.core.tag.*;
import com.biglybt.pif.*;
import com.biglybt.pif.download.*;
import com.biglybt.pif.logging.*;
import com.biglybt.pif.torrent.*;
import com.biglybt.pif.ui.*;
import com.biglybt.pif.ui.config.*;
import com.biglybt.pif.ui.model.*;
import com.biglybt.pifimpl.local.PluginCoreUtils;
import com.biglybt.ui.swt.pif.*;

public class ScanerssAzPlugin implements UnloadablePlugin {

	private static PluginInterface		pluginInterface;
	private static Display				display;
	private static boolean				firstRun;
	private static BasicPluginViewModel	view_model;

	// new API startup code
	private UISWTInstance				swtInstance		= null;
	private View						myView			= null;
	private Logger						logger;
	private LoggerChannel				logChannel;
	private ScheduledExecutorService	scheduler		= Executors
																.newScheduledThreadPool(
																		1,
																		new ThreadFactory() {
																			public Thread newThread (
																					Runnable r) {
																				Thread t = new Thread(
																						r);
																				t
																						.setDaemon(true);
																				return t;
																			}
																		});

	private UIManagerListener			uiListener;
	private PluginListener				pluginListener;
	private DownloadManagerListener		dlmListener;
	private DownloadListener			dlListener;
	private LoggerChannelListener		logListener;
	private BasicPluginConfigModel		config_model;
	public static TorrentAttribute		TA_MOVE_TO;
	public static TorrentAttribute		TA_RENAME_TO;
	public static TorrentAttribute		TA_SCANERSS;
	private static DecimalFormat		format2Digit	= new DecimalFormat(
																"##00");
	private static DecimalFormat		format3Digit	= new DecimalFormat(
																"#000");

	public void initialize (final PluginInterface pi) {

		pluginInterface = pi;

		TA_MOVE_TO = pluginInterface.getTorrentManager().getPluginAttribute(
				"MoveToDirectoryAfterCompletion");
		TA_RENAME_TO = pluginInterface.getTorrentManager().getPluginAttribute(
				"RenameToDirectoryAfterRemove");
		TA_SCANERSS = pluginInterface.getTorrentManager().getPluginAttribute(
				"AddedByScaneRSS");

		UIManager ui_manager = pluginInterface.getUIManager();

		logger = pluginInterface.getLogger();
		logChannel = logger.getTimeStampedChannel("ScaneRSS");

		firstRun = generateUID();

		view_model = ui_manager.createBasicPluginViewModel("ScaneRSS Log");
		view_model.getStatus().setVisible(false);

		logListener = new LoggerChannelListener() {
			public void messageLogged (int type, String content) {
				view_model.getLogArea().appendText(content + "\n");
			}

			public void messageLogged (String str, Throwable error) {
				if (str.length() > 0) {
					view_model.getLogArea().appendText(str + "\n");
				}

				StringWriter sw = new StringWriter();

				PrintWriter pw = new PrintWriter(sw);

				error.printStackTrace(pw);

				pw.flush();

				view_model.getLogArea().appendText(sw.toString() + "\n");
			}
		};

		logChannel.addListener(logListener);

		config_model = ui_manager.createBasicPluginConfigModel("plugins",
				"plugin.scanerss");

		// settings on main options panel
		config_model.addBooleanParameter2("scanerss_military_time",
				"scanerss.military.time", false);
		config_model.addBooleanParameter2("scanerss_auto_open",
				"scanerss.auto.open", true);
		BooleanParameter enableLog = config_model.addBooleanParameter2(
				"logDownloads", "scanerss.logDownloads", false);
		Parameter logFile = config_model.addStringParameter2("logFile",
				"scanerss.logFile", "downloads.log");
		enableLog.addEnabledOnSelection(logFile);
		config_model.addBooleanParameter2("useCustomFeedIcons",
				"scanerss.useCustomFeedIcons", true);
		config_model.addBooleanParameter2("handle.manually.added.torrents",
				"scanerss.handle.manually.added.torrents", true);
		config_model.addLabelParameter2("scanerss.statistics.label");
		config_model.addBooleanParameter2("statistics.allow",
				"scanerss.statistics.allow", true);

		myView = new View(pluginInterface, this);
		try {
			myView.setRssFileAndLoad(new File(pluginInterface
					.getPluginDirectoryName()
					+ System.getProperty("file.separator") + "rss.xml.gz"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		uiListener = new UIManagerListener() {
			public void UIAttached (UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					swtInstance = (UISWTInstance) instance;
					display = swtInstance.getDisplay();
					swtInstance.addView(UISWTInstance.VIEW_MAIN, View.VIEWID,
							myView);
					if (isPluginAutoOpen()) {
						swtInstance.openMainView(View.VIEWID, myView, null);
						getLoggerChannel().log("Opening Main View!");
					}
				}
			}

			public void UIDetached (UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					swtInstance = null;
				}
			}
		};

		pluginInterface.getUIManager().addUIListener(uiListener);

		pluginListener = new PluginListener() {
			public void closedownComplete () {
			}

			public void closedownInitiated () {
				myView.save();
			}

			public void initializationComplete () {
				myView.getFeedManager().updateAllFeedsAndRespectInterval();
			}
		};

		pluginInterface.addListener(pluginListener);

		dlListener = new DownloadListener() {

			public void stateChanged (Download download, int oldState,
					int newState) {
				if (oldState == Download.ST_DOWNLOADING
						&& download.isComplete()) {

					download.removeListener(this);
					String outDirString = download.getAttribute(TA_MOVE_TO);

					if (outDirString != null) {
						try {
							File outDir = new File(outDirString);
							if (!outDir.exists()) {
								outDir.mkdirs();
							}
							getLoggerChannel().log(
									"Moving " + download.getName() + " to "
											+ outDirString);
							download.moveDataFiles(outDir);
							download.moveTorrentFile(outDir);
						} catch (Exception e) {
							e.printStackTrace();
							getLoggerChannel().log(
									"Failed to move Download: "
											+ e.getMessage());
						}
					}
				}

			}

			public void positionChanged (Download download, int oldPosition,
					int newPosition) {

			}
		};

		dlmListener = new DownloadManagerListener() {
			final TorrentAttribute	ta_category	= pi
														.getTorrentManager()
														.getAttribute(
																TorrentAttribute.TA_CATEGORY);

			public void downloadRemoved (Download download) {
				download.removeListener(dlListener);

				String renameToString = download.getAttribute(TA_RENAME_TO);
				if (renameToString != null
						&& download.getDiskManagerFileInfo().length == 1) {

					final File downloadFile = download.getDiskManagerFileInfo()[0]
							.getFile();

					String dlName = downloadFile.getName();
					String ext = dlName.substring(dlName.lastIndexOf('.'));
					final String newName = renameToString + ext;

					scheduler.schedule(new Runnable() {

						public void run () {
							try {
								downloadFile.renameTo(new File(downloadFile
										.getParent(), newName));
								getLoggerChannel().log(
										"Renaming [" + downloadFile.getName()
												+ "] to [" + newName + "]");
							} catch (Exception e) {
								e.printStackTrace();
								getLoggerChannel().log(
										"Failed to rename Download: "
												+ e.getMessage());
							}
						}
					}, 60, TimeUnit.SECONDS);
				}
			}

			public void downloadAdded (Download download) {
				boolean handleManually = pluginInterface.getPluginconfig()
						.getPluginBooleanParameter(
								"handle.manually.added.torrents");

				if (handleManually && !download.isComplete()
						&& !download.getBooleanAttribute(TA_SCANERSS)) {

					getLoggerChannel().log(
							"Found manually added Download checking filters: "
									+ download.getName());

					Filter[] filters = myView.getFeedManager()
							.getFilterAsArray();

					String url = "http://empty.url";
					String downloadName = download.getName();
					for (Filter f : filters) {

						if (f.apply(downloadName, url)) {

							getLoggerChannel().log(
									"Filter matched: " + f.getName());

							String outputDir = f.getOutputDir();
							String renameTo = f.getRenameTo();
							String category_or_tag_name = f.getCategory();

							if (outputDir != null) {
								outputDir = replacePlaceholders(outputDir,
										downloadName, f);
								download.setAttribute(
										ScanerssAzPlugin.TA_MOVE_TO, outputDir);
								getLoggerChannel().log(
										"Setting move after completion dir to: "
												+ outputDir);
							}

							if (renameTo != null) {
								renameTo = replacePlaceholders(renameTo,
										downloadName, f);
								download
										.setAttribute(
												ScanerssAzPlugin.TA_RENAME_TO,
												renameTo);
								getLoggerChannel().log(
										"Setting renameTo after completion to: "
												+ renameTo);
							}

							if (category_or_tag_name != null) {
								
								if ( CategoryManager.getCategory( category_or_tag_name ) == null ){
									
									try{
										TagType tt = TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL );
										
										String[] tag_names = category_or_tag_name.replace( ';', ',' ).split( "," );
										
										for ( String tag_name: tag_names ){
											
											tag_name = tag_name.trim();
											
											if ( tag_name.length() == 0 ){
												
												continue;
											}
											
											Tag tag = tt.getTag( tag_name, true );
											
											if ( tag == null ){
												
												try{
													tag = tt.createTag( tag_name, true );
													
												}catch( Throwable e ){
													
													Debug.out( e );
												}
											}
		
											if ( tag != null && !( tag.isTagAuto()[0] || tag.isTagAuto()[1])){
												
												tag.addTaggable( PluginCoreUtils.unwrap( download ));
												
												getLoggerChannel().log(
														"Adding tag : " + tag_name);
											}
										}
									}catch( Throwable e ){
										
										e.printStackTrace();
									}
								}else{
								
									download.setAttribute(ta_category, category_or_tag_name);
									
									getLoggerChannel().log(
											"Setting category to: " + category_or_tag_name);
								}
							}
						}
					}
				}

				download.addListener(dlListener);
			}
		};

		pluginInterface.getDownloadManager().addListener(dlmListener);

		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run () {
				try {
					myView.getFeedManager().updateAllFeedsAndRespectInterval();
					myView.retryFailedDownloads();
					logStat();
				} catch (RuntimeException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}, 10, 60, TimeUnit.SECONDS);

	}

	/**
	 * Gets the pluginInterface from Plugin.java
	 * 
	 * @return pluginInterface
	 */
	public static PluginInterface getPluginInterface () {
		return pluginInterface;
	}

	/**
	 * Gets the Display from Plugin.java from the UISWTInstance
	 * 
	 * @return display
	 */
	public static Display getDisplay () {
		return display;
	}

	private boolean generateUID () {
		if (!pluginInterface.getPluginconfig().getPluginStringParameter(
				"scanerss.uid").equals("")) {
			return false;
		}

		long n = System.currentTimeMillis();
		byte[] b = new byte[8];
		b[7] = (byte) (n);
		n >>>= 8;
		b[6] = (byte) (n);
		n >>>= 8;
		b[5] = (byte) (n);
		n >>>= 8;
		b[4] = (byte) (n);
		n >>>= 8;
		b[3] = (byte) (n);
		n >>>= 8;
		b[2] = (byte) (n);
		n >>>= 8;
		b[1] = (byte) (n);
		n >>>= 8;
		b[0] = (byte) (n);
		try {
			String uid = CryptoTools.formatByte(CryptoTools.messageDigest(b,
					"SHA-1"), true);
			pluginInterface.getPluginconfig().setPluginParameter(
					"scanerss.uid", uid);
			return true;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static String replacePlaceholders (String input, String name,
			Filter filter) {
		Episode e = new Episode(name);

		input = input.replace("{S}", String.valueOf(e.getSeason())).replace(
				"{SS}", format2Digit.format(e.getSeason())).replace("{E}",
				String.valueOf(e.getEpisode())).replace("{EE}",
				format2Digit.format(e.getEpisode())).replace("{EEE}",
				format3Digit.format(e.getEpisode()));

		if (filter != null) {
			input = input.replace("{FilterName}", filter.getName());
		}
		return input;
	}

	/**
	 * Returns the user set status of whether or not the plugin should autoOpen
	 * 
	 * @return boolean autoOpen
	 */
	public static boolean isPluginAutoOpen () {
		PluginConfig config_getter = getPluginInterface().getPluginconfig();
		return config_getter.getPluginBooleanParameter("scanerss_auto_open",
				true);
	}

	public Element ipcGetFeedManagerAsElement () {
		return myView.getFeedManager().toElement();
	}

	public boolean ipcSetFeedManagerFromElement (Element e) {
		RSSFeedManager fm = myView.getFeedManager();
		fm.reset();
		fm.readFromElement(e);
		myView.save();
		return true;
	}

	/**
	 * Provides a way to save the Config via IPC.
	 * 
	 * This is intendet to be used with TaskManager
	 * 
	 * @param filename save config here
	 * @return true on success, false on error
	 */
	public boolean ipcBackupConfigTo (String filename) {
		try {
			myView.saveTo(filename);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	// Return the master log channel
	public LoggerChannel getLoggerChannel () {
		return logChannel;
	}

	/**
	 * @return the view_model
	 */
	public static BasicPluginViewModel getView_model () {
		return view_model;
	}

	public void logStat () {
		if (pluginInterface.getPluginconfig().getPluginBooleanParameter(
				"statistics.allow")) {
			long lastcheck = Long.parseLong(pluginInterface.getPluginconfig()
					.getPluginStringParameter("stats.lastcheck", "0"));
			if (System.currentTimeMillis() - lastcheck > 1000 * 60 * 60 * 24) {
				Thread t = new Thread() {
					@Override
					public void run () {
						try {
							URL url = new URL(
									"http://azsmrc.sourceforge.net/info.php?app=ScaneRSS&version="
											+ pluginInterface
													.getPluginVersion()
											+ "&uid="
											+ pluginInterface.getPluginconfig()
													.getPluginStringParameter(
															"scanerss.uid"));

							System.out.println(url.toExternalForm());
							HttpURLConnection conn = (HttpURLConnection) url
									.openConnection();
							conn.connect();
							conn.getResponseCode();
							conn.disconnect();
							pluginInterface
									.getPluginconfig()
									.setPluginParameter(
											"stats.lastcheck",
											Long
													.toString(System
															.currentTimeMillis() - 60000));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				t.setDaemon(true);
				t.setPriority(Thread.MIN_PRIORITY);
				t.start();
			}
		}
	}

	/**
	 * @return the firstRun
	 */
	public static boolean isFirstRun () {
		return firstRun;
	}

	/**
	 * @param firstRun the firstRun to set
	 */
	public static void setFirstRun (boolean firstRun) {
		ScanerssAzPlugin.firstRun = firstRun;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.plugins.UnloadablePlugin#unload()
	 */
	public void unload () throws PluginException {
		try {
			scheduler.shutdown();
		} catch (AccessControlException e) {
			logChannel.log(e);
		}
		myView.save();
		swtInstance.removeViews(UISWTInstance.VIEW_MAIN, View.VIEWID);
		pluginInterface.removeListener(pluginListener);
		pluginInterface.getUIManager().removeUIListener(uiListener);
		pluginInterface.getDownloadManager().removeListener(dlmListener, true);
		logChannel.removeListener(logListener);
		config_model.destroy();
		view_model.destroy();
	}
	// EOF
}