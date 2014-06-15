// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler.proxy;

import java.net.Proxy;
import java.net.InetSocketAddress;

import org.apache.http.auth.Credentials;

public class AuthenticatedProxy extends Proxy {

	public AuthenticatedProxy(Type type, InetSocketAddress sa, Credentials auth) {
		super(type, sa);
		_address = sa;
		_authentication = auth;
	}

	private final Credentials _authentication;
	private final InetSocketAddress _address;

	public InetSocketAddress getInetSocketAddress() {
		return _address;
	}

	public Credentials getCredentials() {
		return _authentication;
	}

}
