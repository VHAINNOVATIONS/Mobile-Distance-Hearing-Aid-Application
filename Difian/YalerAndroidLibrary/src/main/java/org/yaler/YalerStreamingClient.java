// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import org.yaler.util.StreamHelper;

public class YalerStreamingClient { // TODO: rename to YalerStreamingSocketFactory?
	private YalerStreamingClient () {}

	public static final Socket connectSSLSocket (
		String relayHost, int relayPort, String relayDomain, Proxy proxy)
		throws IOException
	{
		String host = relayHost;
		int port = relayPort;
		Socket result;
		boolean connected;
		int[] x = new int[3];
		do {
			result = YalerSSLSocketFactory.getInstance().createSocket(proxy, host, port, 5000);
            result.setSoTimeout(5000);
            result.setTcpNoDelay(true);
            InputStream in = result.getInputStream();
			OutputStream out = result.getOutputStream();
			out.write((
                "OPTIONS /" + relayDomain + " HTTP/1.1\r\n" +
                "Upgrade: plainsocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Host: " + host + "\r\n\r\n").getBytes());
			out.flush();
			for (int i = 0; i != 12; i++) {
				x[i % 3] = in.read();
			}
			if ((x[0] == '3') && (x[1] == '0') && (x[2] == '7')) {
				InetSocketAddress address = StreamHelper.findLocation(in, "https", 443);
				host = address.getHostName();
				port = address.getPort();
			}
			connected = StreamHelper.find(in, "\r\n\r\n");
			if (!connected || (x[0] != '1') || (x[1] != '0') || (x[2] != '1')) {
				result.close();
				result = null;
			}
		} while (connected && ((x[0] == '3') && (x[1] == '0') && (x[2] == '7')));
		return result;
	}
}
