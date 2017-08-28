package lbms.plugins.scanerss.azureus;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.biglybt.pif.torrent.*;

import lbms.plugins.scanerss.main.ITorrent;
import lbms.plugins.scanerss.main.ITorrentFile;

/**
 * @author Damokles
 *
 */
public class TorrentAdapter implements ITorrent {

	private Torrent torrent;

	public TorrentAdapter (Torrent t) {
		torrent = t;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.scanerss.main.ITorrent#getFiles()
	 */
	public ITorrentFile[] getFiles() {
		TorrentFile[] tfiles = torrent.getFiles();
		TorrentFileAdapter[] files = new TorrentFileAdapter[tfiles.length];
		for (int i = 0; i<tfiles.length; i++) {
			files[i] = new TorrentFileAdapter(tfiles[i]);
		}
		return files;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.scanerss.main.ITorrent#getName()
	 */
	public String getName() {
		return torrent.getName();
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.scanerss.main.ITorrent#getSize()
	 */
	public long getSize() {
		return torrent.getSize();
	}

	/**
	 * @return the torrent
	 */
	public Torrent getTorrent() {
		return torrent;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.scanerss.main.ITorrent#getAnnounceUrl()
	 */
	public String getAnnounceUrl() {
		return torrent.getAnnounceURL().toExternalForm();
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.scanerss.main.ITorrent#getInfoHash()
	 */
	public String getInfoHash() {
		try {
			return URLEncoder.encode(
					new String(torrent.getHash(), "ISO-8859-1"),
					"ISO-8859-1").replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} return "";
	}
}
