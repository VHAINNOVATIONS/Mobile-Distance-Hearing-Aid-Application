// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler.proxy;

import java.io.IOException;
import java.net.Proxy;
import java.net.InetSocketAddress;

public class ProxyAuthenticationException extends IOException {
	private static final long serialVersionUID = 1L;

    public final String _proxyHostName;

    public ProxyAuthenticationException(Proxy proxy) {
		this(proxy, null);
	}

	public ProxyAuthenticationException(Proxy proxy, Throwable cause) {
		super("Failed to authenticate to the proxy " + ((InetSocketAddress)proxy.address()).getHostName(), cause);
		_proxyHostName = ((InetSocketAddress)proxy.address()).getHostName();
	}

	public ProxyAuthenticationException(String message, Proxy proxy) {
		super(message + " {Proxy:"+((InetSocketAddress)proxy.address()).getHostName()+"}");
		_proxyHostName = ((InetSocketAddress)proxy.address()).getHostName();
	}
}
