// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import org.yaler.proxy.ProxyClient;
import org.yaler.util.MalformedHttpResponseException;

public class YalerSocketFactory {
	YalerSocketFactory() {}

	private static YalerSocketFactory factory;
	public static YalerSocketFactory getInstance() {
		if (factory == null) {
			synchronized (YalerSocketFactory.class) {
				if (factory == null) {
					factory = new YalerSocketFactory();
				}
			}
		}
		return factory;
	}

	public Socket createSocket(Proxy proxy, String host, int port, int timeout) throws IOException {
		Socket socket = null;
		if (proxy != null) {
			if (proxy.type() == Proxy.Type.HTTP) {
				try {
					socket = ProxyClient.connectProxy(proxy, host, port, timeout);
				} catch (MalformedHttpResponseException e) {
					throw new IOException("proxy connection failed", e);
				}
			} else {
				socket = new Socket(proxy);
                socket.setSoTimeout(timeout);
                socket.connect(new InetSocketAddress(host, port), timeout);
			}
		} else {
			socket = new Socket();
            socket.setSoTimeout(timeout);
            socket.connect(new InetSocketAddress(host, port), timeout);
		}
		return socket;
	}

}
