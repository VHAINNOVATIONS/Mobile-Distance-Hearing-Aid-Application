// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler.test;

import org.yaler.YalerSSLServerSocket;
import org.yaler.YalerSSLSocketFactory;

public class YalerSSLServerSocketTest extends YalerServerSocketTestBase {
	@Override
	protected void setUp () {
		super.setUp();
		_relayProtocol = "https";
		_server = new YalerSSLServerSocket(_relayHost, 443, _relayDomain);
		_unreachableServer = new YalerSSLServerSocket(
			_unreachableRelayHost, 443, _relayDomain);
		_socketFactory = YalerSSLSocketFactory.getInstance();
	}
}
