package de.deltatree.pc.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.extras.SelfSignedMitmManager;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

@Slf4j
public class DTPCProxy {

	private final HttpProxyServer server;

	public DTPCProxy() {
		this.server = createFilteredServer();
	}

	private HttpProxyServer createFilteredServer() {
		ProxyAuthenticator proxyAuthenticator = new ProxyAuthenticator() {
			public boolean authenticate(String userName, String password) {
				log.info("authenticating user {}", userName);
				return true;
			}
		};
		return DefaultHttpProxyServer
				.bootstrap()
				.withPort(8080)
				.withProxyAuthenticator(proxyAuthenticator)
				.withFiltersSource(new HttpFiltersSourceAdapter() {
					public HttpFilters filterRequest(
							HttpRequest originalRequest,
							ChannelHandlerContext ctx) {
						return new HttpFiltersAdapter(originalRequest) {
							@Override
							public HttpResponse requestPre(HttpObject httpObject) {

								return null;
							}

							@Override
							public HttpResponse requestPost(
									HttpObject httpObject) {
								return null;
							}

							@Override
							public HttpObject responsePre(HttpObject httpObject) {
								return httpObject;
							}

							@Override
							public HttpObject responsePost(HttpObject httpObject) {
								return httpObject;
							}
						};
					}
				}).withManInTheMiddle(new SelfSignedMitmManager())
				.withAuthenticateSslClients(true).withTransparent(true).start();
	}

	public void stop() {
		this.server.stop();
	}
}
