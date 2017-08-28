/**
 * 
 */
package lbms.tools;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author Leonard
 * 
 */
public class DefaultHttpURLConnectionFactory implements
		HttpURLConnectionFactory {

	static SSLSocketFactory	socFac;

	static {
		//all trusting Trustmanager
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers () {
				return null;
			}

			public void checkClientTrusted (
					java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted (
					java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			socFac = sc.getSocketFactory();
		} catch (Exception e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see lbms.tools.HttpURLConnectionFactory#getConnection(java.net.URL,
	 * java.net.Proxy)
	 */
	@Override
	public HttpURLConnection getConnection (URL url, Proxy proxy)
			throws IOException {
		HttpURLConnection conn;
		if (url.getProtocol().equalsIgnoreCase("https")) {
			// see ConfigurationChecker for SSL client defaults
			HttpsURLConnection ssl_con;
			if (proxy != null) {
				ssl_con = (HttpsURLConnection) url.openConnection(proxy);
			} else {
				ssl_con = (HttpsURLConnection) url.openConnection();
				// allow for certs that contain IP addresses rather than dns names
			}

			ssl_con.setHostnameVerifier(new HostnameVerifier() {
				public boolean verify (String host, SSLSession session) {
					return (true);
				}
			});

			ssl_con.setSSLSocketFactory(socFac);

			conn = ssl_con;
		} else {
			if (proxy != null) {
				conn = (HttpURLConnection) url.openConnection(proxy);
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}
		}
		return conn;
	}
}
