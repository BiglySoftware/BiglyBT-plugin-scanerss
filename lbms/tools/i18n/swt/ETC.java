package lbms.tools.i18n.swt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.eclipse.swt.widgets.Display;

public class ETC {

	protected Display	display;
	protected boolean	terminated;
	private MainWindow	mainWindow;
	private Properties	properties;

	private static ETC	etc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		start();
	}

	public static void start() {
		if (etc == null) {
			etc = new ETC();
		}

		etc.open();

	}

	public ETC() {
		System.out.println("Starting up ETC");
		properties = new Properties();
		File propFile = new File("./ETC.properties");
		if (propFile.exists()) {
			InputStream is = null;
			try {
				is = new FileInputStream(propFile);
				properties.loadFromXML(is);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
			}

		}
	}

	public void open() {
		display = Display.getDefault();
		terminated = false;

		mainWindow = MainWindow.open();

		while (!terminated) { // runnig
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public void close() {
		File propFile = new File("./ETC.properties");
		if (propFile.exists()) {
			OutputStream os = null;
			try {
				os = new FileOutputStream(propFile);
				properties.storeToXML(os, "ETC Properties");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
					}
				}
			}

		}
		terminated = true;
	}

	public Display getDisplay() {
		return display;
	}

	public static ETC getETC() {
		return etc;
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public static Properties getProperties() {
		return etc.properties;
	}

}// EOF
