package lbms.tools.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatsStreamGlobalManager {
	private static List<StatsInputStream> iList = Collections.synchronizedList(new ArrayList<StatsInputStream>());
	private static List<StatsOutputStream> oList = Collections.synchronizedList(new ArrayList<StatsOutputStream>());

	private static long legacyBytesUploaded = 0;
	private static long legacyBytesDownloaded = 0;

	protected static void registerIS (StatsInputStream is) {
		iList.add(is);
	}

	protected static void registerOS (StatsOutputStream os) {
		oList.add(os);
	}

	protected static void removeIS (StatsInputStream is) {
		legacyBytesDownloaded += is.getBytesRead();
		iList.remove(is);
	}

	protected static void removeOS (StatsOutputStream os) {
		legacyBytesUploaded += os.getBytesWritten();
		oList.remove(os);
	}

	public static long getBpsUpload () {
		long result = 0;
		for (StatsOutputStream sos:oList) {
			result += sos.getBytesPerSec();
		}
		return result;
	}

	public static long getBpsDownload () {
		long result = 0;
		for (StatsInputStream sis:iList) {
			result += sis.getBytesPerSec();
		}
		return result;
	}

	public static long getTotalUpload () {
		long result = legacyBytesUploaded;
		for (StatsOutputStream sos:oList) {
			result += sos.getBytesPerSec();
		}
		return result;
	}

	public static long getTotalDownload () {
		long result = legacyBytesDownloaded;
		for (StatsInputStream sis:iList) {
			result += sis.getBytesPerSec();
		}
		return result;
	}

	private StatsStreamGlobalManager() {}
}
