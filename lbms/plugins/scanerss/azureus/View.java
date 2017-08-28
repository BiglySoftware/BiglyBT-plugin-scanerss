package lbms.plugins.scanerss.azureus;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;

import lbms.plugins.scanerss.main.ActionProvider;
import lbms.plugins.scanerss.main.Episode;
import lbms.plugins.scanerss.main.Filter;
import lbms.plugins.scanerss.main.ITorrent;
import lbms.plugins.scanerss.main.RSSFeed;
import lbms.plugins.scanerss.main.RSSItem;
import lbms.plugins.scanerss.main.gui.GUI;
import lbms.tools.DownloadListener;
import lbms.tools.TorrentDownload;

import org.eclipse.swt.widgets.Composite;
import com.biglybt.core.util.*;
import com.biglybt.core.category.*;
import com.biglybt.core.tag.*;
import com.biglybt.pif.*;
import com.biglybt.pif.download.*;
import com.biglybt.pif.ipc.IPCException;
import com.biglybt.pif.ipc.IPCInterface;
import com.biglybt.pif.logging.*;
import com.biglybt.pif.torrent.*;
import com.biglybt.pif.ui.*;
import com.biglybt.pif.ui.config.*;
import com.biglybt.pif.ui.model.*;
import com.biglybt.pifimpl.local.PluginCoreUtils;
import com.biglybt.ui.swt.pif.*;

public class View extends GUI implements UISWTViewEventListener {

	private File					rssFile;
	private LoggerChannel			logger;
	boolean							isCreated			= false;
	public static final String		VIEWID				= "scanerss_View";
	public static final String		AzSMRC_PLUGIN_ID	= "azsmrc";
	private static DecimalFormat	format2Digit		= new DecimalFormat(
																"##00");
	private static DecimalFormat	format3Digit		= new DecimalFormat(
																"#000");

	private IPCInterface			ipc;

	// The Plugin interface
	private PluginInterface			pluginInterface;

	public View(PluginInterface pi, ScanerssAzPlugin plugin) {
		final TorrentAttribute ta_category = pi.getTorrentManager()
				.getAttribute(TorrentAttribute.TA_CATEGORY);
		this.pluginInterface = pi;
		new File(pi.getPluginDirectoryName(), "iconcache").mkdirs();
		this.logger = plugin.getLoggerChannel();
		this.actionProvider = new ActionProvider() {
			private String	defaultUserAgent	= "ScaneRSS/"
														+ pluginInterface
																.getPluginVersion()
														+ " (Azureus/"
														+ pluginInterface
																.getApplicationVersion()
														+ ")";

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#getDefaultUserAgent()
			 */
			@Override
			public String getDefaultUserAgent () {
				return defaultUserAgent;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.scanerss.main.ActionProvider#getPluginDir()
			 */
			@Override
			public String getPluginDir () {
				return pluginInterface.getPluginDirectoryName();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#getTorrent(lbms.plugins
			 * .scanerss.main.RSSItem)
			 */
			@Override
			public ITorrent getTorrent (RSSItem item) throws IOException {
				URL url = null;
				TorrentManager torrentManager = ScanerssAzPlugin
						.getPluginInterface().getTorrentManager();
				Torrent newTorrent = null;
				RSSFeed feed = item.getParentFeed();
				Filter filter = item.getParentFilter();

				if (feed.getModLinkRegex() != null
						&& feed.getModLinkReplace() != null) {
					log(LOG_INFO, "Modifing Link: " + item.getLink());
					String newUrl = feed.getModLinkRegex().matcher(
							item.getLink())
							.replaceAll(feed.getModLinkReplace());
					url = new URL(newUrl);
					log(LOG_INFO, "Modified Link to: " + newUrl);
				} else {
					url = new URL(item.getLink());
				}
				log(LOG_INFO, "Trying to Download Torrent: " + item.getTitle()
						+ " [" + item.getLink() + "] Filter: ["
						+ (filter != null ? filter.getName() : "MANUAL")
						+ "] Feed: [" + feed.getName() + "]");
				TorrentDownload dl = new TorrentDownload(url);
				dl.addDownloadListener(new DownloadListener() {
					public void debugMsg (String msg) {
						logger.log(msg);
					}

					public void progress (long bytesRead, long bytesTotal) {
						ScanerssAzPlugin.getView_model().getProgress()
								.setPercentageComplete(
										(int) ((bytesRead * 100) / bytesTotal));
					}

					public void stateChanged (int oldState, int newState) {
						switch (newState) {
						case lbms.tools.Download.STATE_INITIALIZING:
							ScanerssAzPlugin.getView_model().getActivity()
									.setText("Initializing Download");
							ScanerssAzPlugin.getView_model().getProgress()
									.setPercentageComplete(0);
							break;
						case lbms.tools.Download.STATE_CONNECTING:
							ScanerssAzPlugin.getView_model().getActivity()
									.setText("Connecting");
							break;
						case lbms.tools.Download.STATE_DOWNLOADING:
							ScanerssAzPlugin.getView_model().getActivity()
									.setText("Downloading");
							break;
						case lbms.tools.Download.STATE_FINISHED:
							ScanerssAzPlugin.getView_model().getActivity()
									.setText("Finished");
							ScanerssAzPlugin.getView_model().getProgress()
									.setPercentageComplete(100);
							break;
						case lbms.tools.Download.STATE_FAILURE:
							ScanerssAzPlugin.getView_model().getActivity()
									.setText("Failure");
							break;
						case lbms.tools.Download.STATE_ABORTED:
							ScanerssAzPlugin.getView_model().getActivity()
									.setText("Aborted By User");
							break;
						case lbms.tools.Download.STATE_WAITING:
							ScanerssAzPlugin.getView_model().getActivity()
									.setText("Waiting");
							break;
						case lbms.tools.Download.STATE_RESET:
							ScanerssAzPlugin.getView_model().getActivity()
									.setText("Reset");
							break;
						case lbms.tools.Download.TIMEOUT:
							ScanerssAzPlugin.getView_model().getActivity()
									.setText("Timeout");
							break;
						}
					}
				});
				if (feed.getCookie() != null
						|| (feed.getUsername() != null && feed.getPassword() != null)) {

					if (feed.getCookie() != null) {
						dl.setCookie(feed.getCookie());
					}
					if (feed.getUsername() != null
							&& feed.getPassword() != null) {
						dl.setLogin(feed.getUsername(), feed.getPassword());
					}
					if (feed.getReferer() != null) {
						dl.setReferer(feed.getReferer());
					}

				}
				if (feed.getTorrentLinkIdentifier() != null) {
					dl
							.setTorrentLinkIdentifier(feed
									.getTorrentLinkIdentifier());
				}

				dl.run();
				if (dl.getTorrentLinkIdentifier() != null) {
					feed
							.setTorrentLinkIdentifier(dl
									.getTorrentLinkIdentifier());
				}

				if (dl.hasFailed()) {
					logger.log("Failed to Download Torrent for: "
							+ item.getTitle() + " [" + dl.getFailureReason()
							+ "]");
					throw new IOException("Failed to Download Torrent");
				}

				try {
					if (dl.getReturnCode() == TorrentDownload.RTC_MAGNET) {
						newTorrent = torrentManager.getURLDownloader(
								new URL(dl.getMagnetURL())).download();
					} else if (dl.getReturnCode() == TorrentDownload.RTC_BUFFER) {
						newTorrent = torrentManager.createFromBEncodedData(dl
								.getBuffer().toByteArray());
					} else {
					}
				} catch (TorrentException e) {
					e.printStackTrace();
					throw new IOException(e);
				}

				return new TorrentAdapter(newTorrent);

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#addTorrent(lbms.plugins
			 * .scanerss.main.ITorrent)
			 */
			@Override
			public void addTorrent (RSSItem item, ITorrent tor) {
				Torrent newTorrent = ((TorrentAdapter) tor).getTorrent();
				Filter filter = item.getParentFilter();
				String category_or_tag_name = null;
				String outputDir = null;
				String azsmrcUser = null;
				String renameTo = null;
				try {

					Filter.DownloadState initialState = Filter.DownloadState.QUEUED;
					if (filter != null) {
						renameTo = filter.getRenameTo();
						category_or_tag_name = filter.getCategory();
						outputDir = filter.getOutputDir();
						azsmrcUser = filter.getAzsmrcUser();
						initialState = filter.getInitialState();
					}
					Download tdl = null;
					if (outputDir != null) {
						outputDir = replacePlaceholders(outputDir, item, filter);
						if (filter.isMoveAfterCompletion()) {
							tdl = pluginInterface.getDownloadManager()
									.addDownload(newTorrent);
							tdl.setAttribute(ScanerssAzPlugin.TA_MOVE_TO,
									outputDir);
						} else {
							tdl = pluginInterface.getDownloadManager()
									.addDownload(newTorrent, null,
											new File(outputDir));
						}
					} else {
						tdl = pluginInterface.getDownloadManager().addDownload(
								newTorrent);
					}
					if (renameTo != null) {
						tdl.setAttribute(ScanerssAzPlugin.TA_RENAME_TO,
								replacePlaceholders(renameTo, item, filter));
					}
					log(LOG_INFO, "Download successfuly added: "
							+ item.getTitle());
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
										
										tag.addTaggable( PluginCoreUtils.unwrap( tdl ));
									}
								}
							}catch( Throwable e ){
								
								e.printStackTrace();
							}
						}else{
						
							tdl.setAttribute(ta_category, category_or_tag_name);
						}
					}

					tdl.setBooleanAttribute(ScanerssAzPlugin.TA_SCANERSS, true);

					switch (initialState) {
					case QUEUED:
						tdl.restart();
						break;
					case STOPPED:
						tdl.stop();
						break;
					case FORCEDSTART:
						tdl.setForceStart(true);
						break;
					}

					if (azsmrcUser != null) {
						PluginInterface azPi = ScanerssAzPlugin
								.getPluginInterface().getPluginManager()
								.getPluginInterfaceByID("azsmrc");
						if (azPi != null) {
							try {
								azPi.getIPC().invoke("ipcAddDownloadToUser",
										new Object[] { azsmrcUser, tdl });
							} catch (IPCException e) {
								e.printStackTrace();
							}
						}
					}
				} catch (DownloadException e) {
					e.printStackTrace();
					if (filter != null) {
						filter.removeEpisode(item);
					}
				}
			}

			@Override
			public void addTorrent (final RSSItem item) {
				new Thread(new Runnable() {
					public void run () {
						try {
							addTorrent(item, getTorrent(item));
						} catch (IOException e) {
							e.printStackTrace();
							logger.log("Failed to add Torrent for: "
									+ item.getTitle());
						}
					}

				}).start();
			}

			@Override
			public String getConfigValue (String key) {
				return pluginInterface.getPluginconfig()
						.getPluginStringParameter(key);
			}

			@Override
			public void setConfigValue (String key, String value) {
				pluginInterface.getPluginconfig()
						.setPluginParameter(key, value);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#getConfigValueAsBoolean
			 * (java.lang.String)
			 */
			@Override
			public boolean getConfigValueAsBoolean (String key) {
				return pluginInterface.getPluginconfig()
						.getPluginBooleanParameter(key);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#getConfigValueAsBoolean
			 * (java.lang.String, boolean)
			 */
			@Override
			public boolean getConfigValueAsBoolean (String key, boolean def) {
				return pluginInterface.getPluginconfig()
						.getPluginBooleanParameter(key, def);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#getConfigValueAsInt
			 * (java.lang.String, int)
			 */
			@Override
			public int getConfigValueAsInt (String key, int def) {
				return pluginInterface.getPluginconfig().getPluginIntParameter(
						key, def);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#getConfigValueAsInt
			 * (java.lang.String)
			 */
			@Override
			public int getConfigValueAsInt (String key) {
				return pluginInterface.getPluginconfig().getPluginIntParameter(
						key);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#setConfigValue(java
			 * .lang.String, boolean)
			 */
			@Override
			public void setConfigValue (String key, boolean value) {
				pluginInterface.getPluginconfig()
						.setPluginParameter(key, value);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#setConfigValue(java
			 * .lang.String, int)
			 */
			@Override
			public void setConfigValue (String key, int value) {
				pluginInterface.getPluginconfig()
						.setPluginParameter(key, value);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.scanerss.main.ActionProvider#log(int,
			 * java.lang.String)
			 */
			@Override
			public void log (int level, String msg) {
				switch (level) {
				case LOG_DEBUG:
				case LOG_INFO:
					logger.log(LoggerChannel.LT_INFORMATION, msg);
					break;
				case LOG_WARN:
					logger.log(LoggerChannel.LT_WARNING, msg);
					break;
				case LOG_ERROR:
				case LOG_FATAL:
					logger.log(LoggerChannel.LT_ERROR, msg);
					break;
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.scanerss.main.ActionProvider#log(int,
			 * java.lang.String, java.lang.Object)
			 */
			@Override
			public void log (int level, String msg, Object o) {
				switch (level) {
				case LOG_DEBUG:
				case LOG_INFO:
					logger.log(o, LoggerChannel.LT_INFORMATION, msg);
					break;
				case LOG_WARN:
					logger.log(o, LoggerChannel.LT_WARNING, msg);
					break;
				case LOG_ERROR:
				case LOG_FATAL:
					logger.log(o, LoggerChannel.LT_ERROR, msg);
					break;
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#log(java.lang.String,
			 * java.lang.Throwable)
			 */
			@Override
			public void log (String msg, Throwable e) {
				logger.log(msg, e);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#log(java.lang.String,
			 * java.lang.Throwable, java.lang.Object)
			 */
			@Override
			public void log (String msg, Throwable e, Object o) {
				logger.log(o, msg, e);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * lbms.plugins.scanerss.main.ActionProvider#log(java.lang.Throwable
			 * )
			 */
			@Override
			public void log (Throwable e) {
				logger.log(e);
			}

			@Override
			public boolean isAddTorrentSupported () {
				return true;
			}

			@Override
			public boolean isScrapeTorrentSupported () {
				return false;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.scanerss.main.ActionProvider#saveRSSData()
			 */
			@Override
			protected void saveRSSDataImpl () {
				try {
					feedManager.saveToFile(rssFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		this.feedManager.setActionProvider(actionProvider);
	}

	public void save () {
		actionProvider.saveRSSData();
	}

	public void saveTo (String file) throws IOException {
		feedManager.saveToFile(new File(file));
	}

	public void setRssFileAndLoad (File f) throws IOException {
		this.rssFile = f;
		if (f.exists()) {
			feedManager.loadFromFile(f);
		}
	}

	/**
	 * Here is the GUI initialization
	 */
	public void initialize (Composite parent) {
		createContents(parent);
	}

	/**
	 * Delete function... runs at close of plugin from within Azureus
	 */

	public void delete () {
		isCreated = false;
	}

	public boolean eventOccurred (UISWTViewEvent event) {
		switch (event.getType()) {

		case UISWTViewEvent.TYPE_CREATE:
			if (isCreated) {
				return false;
			}
			isCreated = true;
			break;

		case UISWTViewEvent.TYPE_INITIALIZE:
			initialize((Composite) event.getData());
			if (ScanerssAzPlugin.isFirstRun()) {
				ScanerssAzPlugin.setFirstRun(false);
				StartupWizard.open(((Composite) event.getData()).getDisplay());
			}
			break;

		case UISWTViewEvent.TYPE_DESTROY:
			delete();
			break;

		case UISWTViewEvent.TYPE_FOCUSGAINED:
			updateFeedTreeAndTable(true);
			break;
		}
		return true;
	}

	private static String replacePlaceholders (String input, RSSItem item,
			Filter filter) {
		if (item.getEpisode() != null) {
			Episode e = item.getEpisode();
			input = input.replace("{S}", String.valueOf(e.getSeason()))
					.replace("{SS}", format2Digit.format(e.getSeason()))
					.replace("{E}", String.valueOf(e.getEpisode())).replace(
							"{EE}", format2Digit.format(e.getEpisode()))
					.replace("{EEE}", format3Digit.format(e.getEpisode()));
		}
		if (filter != null) {
			input = input.replace("{FilterName}", filter.getName());
		}
		return input;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see lbms.plugins.scanerss.main.GUI#getAzSMRCUsernames()
	 */
	@Override
	protected String[] getAzSMRCUsernames () {
		if (ipc == null) {
			PluginInterface target = this.pluginInterface.getPluginManager()
					.getPluginInterfaceByID(AzSMRC_PLUGIN_ID);
			if (target != null) {
				ipc = target.getIPC();
			}
		}
		if (ipc != null) {
			try {
				return (String[]) ipc.invoke("ipcGetUsers", new Object[0]);
			} catch (IPCException e) {
				e.printStackTrace();
			}
		}
		return super.getAzSMRCUsernames();
	}

	// EOF
}
