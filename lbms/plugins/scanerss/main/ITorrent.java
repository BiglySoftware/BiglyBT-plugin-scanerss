package lbms.plugins.scanerss.main;

/**
 * @author Damokles
 *
 */
public interface ITorrent {

	public long getSize();
	public String getName();
	public String getAnnounceUrl();
	public String getInfoHash();
	public ITorrentFile[] getFiles();
}
