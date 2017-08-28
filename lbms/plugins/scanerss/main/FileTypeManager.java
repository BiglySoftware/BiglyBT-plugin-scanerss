/**
 * 
 */
package lbms.plugins.scanerss.main;

import java.util.TreeMap;

/**
 * @author Leonard
 * 
 */
public class FileTypeManager {
	private TreeMap<Integer, FileType>	fileTypes	= new TreeMap<Integer, FileType>();

	public FileType addFileType (String name) {
		int id = fileTypes.lastKey() + 1;
		FileType ft = new FileType(name, id);
		fileTypes.put(id, ft);
		return ft;
	}

	public FileType addFileType (String name, String... patterns) {
		int id = fileTypes.lastKey() + 1;
		FileType ft = new FileType(name, id);
		fileTypes.put(id, ft);
		for (String pattern : patterns) {
			ft.addPattern(pattern);
		}
		return ft;
	}

	public void restoreDefaultFileTypes () {
		addFileType("All files", "");

		FileType mp3 = addFileType("MP3 Files", "mp3");
		FileType ogg = addFileType("Ogg Files", "ogg");
		FileType flac = addFileType("FLAC Files", "flac");
		FileType wma = addFileType("Windows Media Audio Files", "wma");
		FileType audio = addFileType("Audio Files");
		audio.addSubType(mp3);
		audio.addSubType(ogg);
		audio.addSubType(flac);
		audio.addSubType(wma);

		FileType xvid = addFileType("Xvid Files", "xvid");
		FileType h264 = addFileType("H.264 Files", "h264", "x264", "h.264");
		FileType divx = addFileType("Divx Files", "divx");
		FileType mpeg = addFileType("MPEG Files", "mpg", "mpeg");
		FileType avi = addFileType("AVI Files", "avi");
		FileType mkv = addFileType("Matroska Files", "mkv");
		FileType ogm = addFileType("Ogg Media Files", "ogm");
		FileType mp4 = addFileType("MP4 Files", "mp4");
		FileType video = addFileType("Video Files");
		video.addSubType(xvid);
		video.addSubType(h264);
		video.addSubType(divx);
		video.addSubType(mpeg);
		video.addSubType(avi);
		video.addSubType(mkv);
		video.addSubType(ogm);
		video.addSubType(mp4);

		FileType zip = addFileType("ZIP Archive", "zip");
		FileType rar = addFileType("RAR Archive", "rar");
		FileType x7z = addFileType("7z Archive", "7z");
		FileType archives = addFileType("Archive Files");
		archives.addSubType(zip);
		archives.addSubType(rar);
		archives.addSubType(x7z);
	}
}
