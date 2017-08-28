/*
 * Created on Nov 16, 2005
 */
package lbms.plugins.scanerss.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import lbms.plugins.scanerss.main.gui.SWTSafeRunnable;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

/**
 * @author omschaub
 * 
 */
public class ImageRepository {

	private static HashMap<String, Image>	images	= new HashMap<String, Image>();
	private static boolean					loaded;

	public static void loadImages (Display display) {
		if (loaded) {
			return;
		}
		loaded = true;
		//put any images here to load on startup
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/Feed-icon_16.png",
				"rssIcon", 255);

		//Toolbar icons
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/cancel22_22.png",
				"cancel", 255);
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/create22_22.png",
				"create", 255);
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/delete22_22.png",
				"delete", 255);
		loadImage(display, "lbms/plugins/scanerss/main/resources/new22_22.png",
				"new", 255);
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/save22_22.png", "save",
				255);
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/redownload22_22.png",
				"redownload", 255);
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/update22_22.png",
				"update", 255);
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/fileopen22_22.png",
				"fileopen", 255);
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/getData22_22.png",
				"getData", 255);
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/sendData22_22.png",
				"sendData", 255);
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/dbAdd22_22.png", "dbAdd",
				255);
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/dbRemove22_22.png",
				"dbRemove", 255);
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/Feed-icon_22.png",
				"feed", 255);

		//Help tab icons
		loadImage(display,
				"lbms/plugins/scanerss/main/resources/amazon-com.png",
				"amazon.com", 255);
		loadImage(display, "lbms/plugins/scanerss/main/resources/paypal.gif",
				"paypal", 255);
	}

	/**
	 * @return the loaded
	 */
	public static boolean isLoaded () {
		return loaded;
	}

	public static boolean hasImage (String name) {
		return images.containsKey(name);
	}

	public static void loadImageThreadSync (final Display display,
			final String res, final String name) {
		if (display != null && !display.isDisposed()) {
			display.syncExec(new SWTSafeRunnable() {
				@Override
				public void runSafe () {
					loadImage(display, res, name, 255);
				}
			});
		}
	}

	public static void loadImageThreadAsync (final Display display,
			final String res, final String name) {
		if (display != null && !display.isDisposed()) {
			display.asyncExec(new SWTSafeRunnable() {
				@Override
				public void runSafe () {
					loadImage(display, res, name, 255);
				}
			});
		}
	}

	public static void loadImageThreadSync (final Display display,
			final File res, final String name) {
		if (display != null && !display.isDisposed()) {
			display.syncExec(new SWTSafeRunnable() {
				@Override
				public void runSafe () {
					loadImage(display, res, name, 255);
				}
			});
		}
	}

	public static void loadImageThreadAsync (final Display display,
			final File res, final String name) {
		if (display != null && !display.isDisposed()) {
			display.asyncExec(new SWTSafeRunnable() {
				@Override
				public void runSafe () {
					loadImage(display, res, name, 255);
				}
			});
		}
	}

	public static Image loadImage (Display display, String res, String name) {
		return loadImage(display, res, name, 255);
	}

	public static Image loadImage (Display display, File res, String name) {
		return loadImage(display, res, name, 255);
	}

	public static Image loadImage (Display display, String res, String name,
			int alpha) {
		Image im = getImage(name);
		if (null == im) {
			InputStream is = ImageRepository.class.getClassLoader()
					.getResourceAsStream(res);
			if (null != is) {
				if (alpha == 255) {
					im = new Image(display, is);
				} else {
					ImageData icone = new ImageData(is);
					icone.alpha = alpha;
					im = new Image(display, icone);
				}
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				images.put(name, im);
			} else {
				System.out
						.println("ImageRepository:loadImage:: Resource not found: "
								+ res);
			}
		}
		return im;
	}

	public static Image loadImage (Display display, File res, String name,
			int alpha) {
		Image im = getImage(name);
		if (null == im) {
			InputStream is = null;
			try {
				is = new FileInputStream(res);
				if (alpha == 255) {
					im = new Image(display, is);
				} else {
					ImageData icone = new ImageData(is);
					icone.alpha = alpha;
					im = new Image(display, icone);
				}
				images.put(name, im);

			} catch (IOException e) {
				e.printStackTrace();
				System.out
						.println("ImageRepository:loadImage:: Resource not found: "
								+ res);
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException e) {
				}
			}

		}
		return im;
	}

	public static void unLoadImages () {
		Iterator iter = images.values().iterator();
		while (iter.hasNext()) {
			Image im = (Image) iter.next();
			im.dispose();
		}
	}

	public static Image getImage (String name) {
		return images.get(name);
	}

}
