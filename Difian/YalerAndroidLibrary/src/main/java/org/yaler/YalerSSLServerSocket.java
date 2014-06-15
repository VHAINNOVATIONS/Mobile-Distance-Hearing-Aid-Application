// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;

import org.yaler.util.StreamHelper;

public final class YalerSSLServerSocket extends YalerServerSocket {
	public YalerSSLServerSocket(String host, int port, String id) {
		super(host, port, id);
	}

	public YalerSSLServerSocket(String host, int port, String id, Proxy proxy) {
		super(host, port, id, proxy);
	}

	@Override
	public YalerSocketFactory getYalerSocketFactory() {
		return YalerSSLSocketFactory.getInstance();
	}

	@Override
	public InetSocketAddress findLocation(InputStream stream) throws IOException {
		return StreamHelper.findLocation(stream, "https", 443);
	}
}
