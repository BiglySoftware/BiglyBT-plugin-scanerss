package lbms.tools.stats;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class StatsOutputStream extends OutputStream {

	private long bytesWritten 		= 0;
	private long lastTime			= 0;
	private long lastBytesWritten	= 0;
	private long bytesPerSec		= 0;
	private OutputStream os;
	private List<StatsStreamSpeedListener> listeners = new ArrayList<StatsStreamSpeedListener>();

	private StatsOutputStream() {
		super();
		// TODO Auto-generated constructor stub
	}

	public StatsOutputStream(OutputStream os) {
		super();
		this.os = os;
		lastTime=System.currentTimeMillis();
		StatsStreamGlobalManager.registerOS(this);
	}

	public StatsOutputStream(OutputStream os, boolean local) {
		super();
		this.os = os;
		lastTime=System.currentTimeMillis();
		if (!local)
			StatsStreamGlobalManager.registerOS(this);
	}

	@Override
	public void write(int b) throws IOException {
		os.write(b);
		bytesWritten++;
		long now = System.currentTimeMillis();
		long diff = now-lastTime ;
		if (diff>=500) {
			bytesPerSec = (long)((bytesWritten-lastBytesWritten)*(500d/(diff)));
			for (StatsStreamSpeedListener l:listeners) {
				l.speedPerSec(bytesPerSec);
			}
			lastBytesWritten = bytesWritten;
			lastTime = now;
		}
	}

	@Override
	public void close() throws IOException {
		StatsStreamGlobalManager.removeOS(this);
		os.close();
		super.close();
	}

	/**
	 * @return Returns the bytesPerSec.
	 */
	public long getBytesPerSec() {
		return bytesPerSec;
	}

	/**
	 * @return Returns the bytesWritten.
	 */
	public long getBytesWritten() {
		return bytesWritten;
	}


	public void addSpeedListener (StatsStreamSpeedListener l) {
		listeners.add(l);
	}

	public void removeSpeedListener (StatsStreamSpeedListener l) {
		listeners.remove(l);
	}
}
