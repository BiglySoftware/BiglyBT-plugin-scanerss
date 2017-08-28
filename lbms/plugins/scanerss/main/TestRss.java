/**
 *
 */
package lbms.plugins.scanerss.main;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import lbms.plugins.scanerss.main.gui.GUI;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Damokles
 * 
 */
public class TestRss {
	public static void main (String[] args) {
		ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
			 */
			public Thread newThread (Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setPriority(Thread.MIN_PRIORITY);
				return t;
			}
		});
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(1, false));
		final GUI gui = new GUI();
		gui.setActionProvider(new ActionProvider() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.scanerss.main.ActionProvider#getConfigValue(java.lang.String)
			 */
			@Override
			public String getConfigValue (String key) {
				// TODO Auto-generated method stub
				return null;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.scanerss.main.ActionProvider#setConfigValue(java.lang.String,
			 *      java.lang.String)
			 */
			@Override
			public void setConfigValue (String key, String value) {
				// TODO Auto-generated method stub

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.scanerss.main.ActionProvider#saveRSSDataImpl()
			 */
			@Override
			protected void saveRSSDataImpl () {
				// TODO Auto-generated method stub

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.scanerss.main.ActionProvider#isAddTorrentSupported()
			 */
			@Override
			public boolean isAddTorrentSupported () {
				// TODO Auto-generated method stub
				return true;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.scanerss.main.ActionProvider#getTorrent(lbms.plugins.scanerss.main.RSSItem)
			 */
			@Override
			public ITorrent getTorrent (RSSItem item) throws IOException {
				// TODO Auto-generated method stub
				return new ITorrent() {
					/*
					 * (non-Javadoc)
					 * 
					 * @see lbms.plugins.scanerss.main.ITorrent#getFiles()
					 */
					public ITorrentFile[] getFiles () {
						// TODO Auto-generated method stub
						return null;
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see lbms.plugins.scanerss.main.ITorrent#getName()
					 */
					public String getName () {
						// TODO Auto-generated method stub
						return null;
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see lbms.plugins.scanerss.main.ITorrent#getSize()
					 */
					public long getSize () {
						// TODO Auto-generated method stub
						return 10485760;
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see lbms.plugins.scanerss.main.ITorrent#getAnnounceUrl()
					 */
					public String getAnnounceUrl () {
						// TODO Auto-generated method stub
						return null;
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see lbms.plugins.scanerss.main.ITorrent#getInfoHash()
					 */
					public String getInfoHash () {
						// TODO Auto-generated method stub
						return null;
					}
				};
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.scanerss.main.ActionProvider#addTorrent(lbms.plugins.scanerss.main.RSSItem,
			 *      lbms.plugins.scanerss.main.ITorrent)
			 */
			@Override
			public void addTorrent (RSSItem item, ITorrent torrent) {
				// TODO Auto-generated method stub
				super.addTorrent(item, torrent);
				System.out.println(item + " was added");
			}
		});
		try {
			File xml = new File("rss.xml");
			if (xml.exists()) {
				gui.getFeedManager().loadFromFile(xml);
			} else {
				RSSFeed rf1 = new RSSFeed("Tokyo Sho", new URL(
						"http://tokyotosho.com/rss.php"));
				RSSFeed rf2 = new RSSFeed("Baka", new URL(
						"http://baka-updates.com/rss.php"));
				gui.getFeedManager().addFeed(rf1);
				gui.getFeedManager().addFeed(rf2);
				Filter f = new Filter("TestFilter",
						".*?Saiunkoku Monogatari.*?", true, false);
				f.addFilterListener(new FilterListener() {
					public void accepted (RSSItem item) {
						System.out.println();
						System.out.println("Accepted: " + item.getTitle()
								+ " ** " + item.getLink());
						System.out.println();
					}
				});
				gui.getFeedManager().addFilter(f);
				gui.getFeedManager().assignFilterToFeed(f, rf1);
			}

			gui.getFeedManager().saveToFile(new File("rss.xml"));

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		timer.scheduleWithFixedDelay(new Runnable() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Runnable#run()
			 */
			public void run () {
				gui.getFeedManager().updateAllFeedsAndRespectInterval();
			}
		}, 20, 60, TimeUnit.SECONDS);

		gui.getFeedManager().updateAllFeeds();
		gui.createContents(shell);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
		try {
			gui.getFeedManager().saveToFile(new File("rss.xml"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
