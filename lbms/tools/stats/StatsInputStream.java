package lbms.tools.stats;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StatsInputStream extends InputStream {

	private long bytesRead		= 0;
	private long lastTime		= 0;
	private long lastBytesRead	= 0;
	private long bytesPerSec	= 0;
	private InputStream is;
	private List<StatsStreamSpeedListener> listeners = new ArrayList<StatsStreamSpeedListener>();

	private StatsInputStream() {
		super();
		// TODO Auto-generated constructor stub
	}

	public StatsInputStream(InputStream is) {
		super();
		this.is = is;
		lastTime = System.currentTimeMillis();
		StatsStreamGlobalManager.registerIS(this);
	}

	public StatsInputStream(InputStream is, boolean local) {
		super();
		this.is = is;
		lastTime = System.currentTimeMillis();
		if (!local)
			StatsStreamGlobalManager.registerIS(this);
	}

	@Override
	public int read() throws IOException {
		bytesRead++;
		long now = System.currentTimeMillis();
		long diff = now-lastTime ;
		if (diff>=500) {
			bytesPerSec = (long)((bytesRead-lastBytesRead)*(500d/(diff)));
			for (StatsStreamSpeedListener l:listeners) {
				l.speedPerSec(bytesPerSec);
			}
			lastBytesRead = bytesRead;
			lastTime = now;
		}

		return is.read();
	}

	@Override
	public void close() throws IOException {
		StatsStreamGlobalManager.removeIS(this);
		is.close();
		super.close();
	}

	/**
	 * @return Returns the bytesRead.
	 */
	public long getBytesRead() {
		return bytesRead;
	}

	/**
	 * @return Returns the bytesPerSec.
	 */
	public long getBytesPerSec() {
		return bytesPerSec;
	}

	public void setTime() {
		lastTime = System.currentTimeMillis();
	}

	public void addSpeedListener (StatsStreamSpeedListener l) {
		listeners.add(l);
	}

	public void removeSpeedListener (StatsStreamSpeedListener l) {
		listeners.remove(l);
	}
}
