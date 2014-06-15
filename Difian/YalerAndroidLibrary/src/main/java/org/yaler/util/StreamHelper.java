// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

public final class StreamHelper {
	private StreamHelper() {}

	public static final String readLine(InputStream stream) throws IOException {
		String line;
		int buffer;
		char[] lineBuffer = new char[4096];
		int lineLength = 0;
		do {
			buffer = stream.read();
			if (buffer != -1 && lineLength != lineBuffer.length) {
				lineBuffer[lineLength] = (char) buffer;
				lineLength++;
			} else if (buffer != -1) {
				throw new IOException("response too long"); //$NON-NLS-1$
			}
		} while (buffer != '\n' && buffer != -1);
		if (buffer == -1) {
			line = null;
		} else if (lineLength >= 2 && lineBuffer[lineLength-2] == '\r') {
			line = new String(lineBuffer, 0, lineLength-2);
		} else {
			line = new String(lineBuffer, 0, lineLength-1);
		}
		return line;
	}

	public static final boolean find(InputStream stream, String pattern) throws IOException {
		byte[] bytePattern = pattern.getBytes();
		int i = 0, j = 0, k = 0, p = 0, c = 0, x = 0;
		while ((k != pattern.length()) && (c != -1)) {
			if (i + k == j) {
				c = x = (byte)stream.read();
				p = i;
				j++;
			} else if (i + k == j - 1) {
				c = x;
			} else {
				c = bytePattern[i + k - p];
			}
			if (bytePattern[k] == c) {
				k++;
			} else {
				k = 0;
				i++;
			}
		}
		return k == pattern.length();
	}

	public static final InetSocketAddress findLocation(InputStream stream, String protocol, int defaultPort) throws IOException {
		InetSocketAddress address = null;
		int port = defaultPort;
		boolean found = find(stream, "\r\nLocation: " + protocol + "://"); //$NON-NLS-1$ //$NON-NLS-2$
		if (found) {
			StringBuilder h = new StringBuilder();
			int x = stream.read();
			while (x != -1 && x != ':' && x != '/') {
				h.append((char)x);
				x = stream.read();
			}
			if (x == ':') {
				port = 0;
				x = stream.read();
				while (x != -1 && x != '/') {
					port = 10 * port + (x-'0');
					x = stream.read();
				}
			}
			String host = h.toString();
			address = InetSocketAddress.createUnresolved(host, port);
		}
		return address;
	}

}
