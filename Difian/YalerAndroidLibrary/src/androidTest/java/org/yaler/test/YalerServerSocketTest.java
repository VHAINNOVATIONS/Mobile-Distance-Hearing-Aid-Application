// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler.test;

import org.yaler.YalerServerSocket;
import org.yaler.YalerSocketFactory;

public class YalerServerSocketTest extends YalerServerSocketTestBase {
	@Override
	protected void setUp () {
		super.setUp();
		_relayProtocol = "http";
		_server = new YalerServerSocket(_relayHost, 80, _relayDomain);
		_unreachableServer = new YalerServerSocket(
				_unreachableRelayHost, 80, _relayDomain);
		_socketFactory = YalerSocketFactory.getInstance();
	}
}
