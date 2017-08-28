package lbms.plugins.scanerss.azureus;


import com.biglybt.pif.torrent.TorrentFile;

import lbms.plugins.scanerss.main.ITorrentFile;

/**
 * @author Damokles
 *
 */
public class TorrentFileAdapter implements ITorrentFile {

	private TorrentFile file;

	public TorrentFileAdapter (TorrentFile f) {
		file = f;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.scanerss.main.ITorrentFile#getName()
	 */
	public String getName() {
		return file.getName();
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.scanerss.main.ITorrentFile#getSize()
	 */
	public long getSize() {
		return file.getSize();
	}

}
