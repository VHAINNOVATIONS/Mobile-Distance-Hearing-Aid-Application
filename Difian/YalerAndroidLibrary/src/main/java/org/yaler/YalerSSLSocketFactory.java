// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler;

import java.io.IOException;
import java.net.Proxy;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.yaler.proxy.ProxyClient;
import org.yaler.util.MalformedHttpResponseException;

public class YalerSSLSocketFactory extends YalerSocketFactory {
	YalerSSLSocketFactory() {}

	private static YalerSSLSocketFactory factory;
	public static YalerSocketFactory getInstance() {
		if (factory == null) {
			synchronized (YalerSSLSocketFactory.class) {
				if (factory == null) {
					factory = new YalerSSLSocketFactory();
				}
			}
		}
		return factory;
	}

	@Override
	public Socket createSocket(Proxy proxy, String host, int port, int timeout) throws IOException {
		Socket socket = null;
		SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		if (proxy == null || proxy.type() == Proxy.Type.DIRECT) {
			socket = sslSocketFactory.createSocket(host, port);
		} else if (proxy.type() == Proxy.Type.SOCKS) {
			Socket proxySocket = new Socket(proxy);
            proxySocket.setSoTimeout(timeout);
			proxySocket.connect(new java.net.InetSocketAddress(host, port), timeout);
			socket = sslSocketFactory.createSocket(proxySocket, host, port, true);
		} else if (proxy.type() == Proxy.Type.HTTP) {
			Socket proxySocket = null;
			try {
				proxySocket = ProxyClient.connectProxy(proxy, host, port, timeout);
			} catch (MalformedHttpResponseException e) {
				throw new IOException("proxy connection failed", e);
			}
			SSLSocket sslsocket = (SSLSocket) sslSocketFactory.createSocket(proxySocket, host, port, true);
			sslsocket.startHandshake();
			socket = sslsocket;
		}
		return socket;
	}

}
