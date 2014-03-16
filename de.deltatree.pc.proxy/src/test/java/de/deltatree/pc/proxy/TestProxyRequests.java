package de.deltatree.pc.proxy;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.PropertyConfigurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;

@Slf4j
public class TestProxyRequests {

	private static DTPCProxy proxy;
	private static CloseableHttpClient httpclient;

	@BeforeClass
	public static void setUp() throws Exception {
		PropertyConfigurator.configureAndWatch("log4j.conf");

		log.info("starting server / init client...");
		proxy = new DTPCProxy();
		httpclient = createHttpClient();
		log.info("server started / client initiated.");
	}

	@AfterClass
	public static void tearDown() throws Exception {
		log.info("server is shutting down...");
		proxy.stop();
		httpclient.close();
		log.info("shut down.");
	}

	@Test
	public void testHttp() throws ClientProtocolException, IOException,
			InterruptedException, KeyManagementException,
			NoSuchAlgorithmException, KeyStoreException {
		HttpHost target = new HttpHost("www.google.de", 80, "http");
		HttpHost proxy = new HttpHost("localhost", 8080, "http");
		String data = exec(httpclient, target, proxy);
		log(data);
		assertTrue(!data.contains("407 Proxy Authentication Required"));
	}

	private void log(String data) {
		System.out.println(Strings.repeat("*", 66));
		System.out.println(data);
		System.out.println(Strings.repeat("*", 66));
	}

	@Test
	public void testHttps() throws ClientProtocolException, IOException,
			InterruptedException, KeyManagementException,
			NoSuchAlgorithmException, KeyStoreException {
		HttpHost target = new HttpHost("www.google.de", 443, "https");
		HttpHost proxy = new HttpHost("localhost", 8080, "http");
		String data = exec(httpclient, target, proxy);
		log(data);
		assertTrue(!data.contains("407 Proxy Authentication Required"));
	}

	private String exec(CloseableHttpClient httpclient, HttpHost target,
			HttpHost proxy) throws IOException, ClientProtocolException {
		RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
		HttpGet request = new HttpGet("/");
		request.setConfig(config);

		CloseableHttpResponse response = httpclient.execute(target, request);
		try {
			return EntityUtils.toString(response.getEntity());
		} finally {
			response.close();
		}
	}

	private static CloseableHttpClient createHttpClient()
			throws NoSuchAlgorithmException, KeyStoreException,
			KeyManagementException {
		SSLContextBuilder builder = SSLContexts.custom();
		builder.loadTrustMaterial(null, new TrustStrategy() {
			public boolean isTrusted(X509Certificate[] chain, String authType)
					throws CertificateException {
				return true;
			}
		});
		SSLContext sslContext = builder.build();
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
				sslContext, new X509HostnameVerifier() {

					public boolean verify(String arg0, SSLSession arg1) {
						return true;
					}

					public void verify(String host, SSLSocket ssl)
							throws IOException {
						/* do nothing */
					}

					public void verify(String host, X509Certificate cert)
							throws SSLException {
						/* do nothing */
					}

					public void verify(String host, String[] cns,
							String[] subjectAlts) throws SSLException {
						/* do nothing */
					}
				});

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
				.<ConnectionSocketFactory> create().register("https", sslsf)
				.register("http", new PlainConnectionSocketFactory()).build();

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(
				socketFactoryRegistry);

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope("localhost", 8080),
				new UsernamePasswordCredentials("username", "password"));

		return HttpClients.custom().setConnectionManager(cm)
				.setDefaultCredentialsProvider(credsProvider).build();
	}

}
