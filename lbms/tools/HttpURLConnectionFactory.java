/**
 * 
 */
package lbms.tools;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

/**
 * @author Leonard
 * 
 */
public interface HttpURLConnectionFactory {
	public HttpURLConnection getConnection (URL url, Proxy proxy)
			throws IOException;
}
