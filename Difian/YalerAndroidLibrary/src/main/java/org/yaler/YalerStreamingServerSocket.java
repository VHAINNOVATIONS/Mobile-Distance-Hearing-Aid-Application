// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import org.yaler.util.StreamHelper;

public final class YalerStreamingServerSocket implements Closeable {
	private final YalerServerSocket _baseListener;

	private volatile boolean _closed;
	private volatile Socket _listener;

	public YalerStreamingServerSocket(YalerServerSocket base) {
		_baseListener = base;
	}

    public Socket accept() throws IOException {
        return accept(null);
    }

    public Socket accept(AcceptCallback acceptCallback) throws IOException {
		if (_closed) {
			throw new SocketException("YalerStreamingServerSocket is closed");
		}
		try {
			boolean acceptable = false;
			Socket result = null;
			_listener = _baseListener.accept(acceptCallback);
			if (!_closed) {
				_listener.setSoTimeout(75000);
				result = _listener;
				InputStream i = result.getInputStream();
				OutputStream o = result.getOutputStream();
				acceptable = StreamHelper.find(i, "\r\n\r\n");
				if (acceptable) {
					o.write((
						"HTTP/1.1 101 Switching Protocols\r\n" +
			    		"Upgrade: plainsocket\r\n" +
			    		"Connection: Upgrade\r\n\r\n").getBytes());
					result.setSoTimeout(0);
				} else {
					result.close();
					result = null;
				}
			}
			return result;
		} finally {
			_listener = null;
		}
	}

	@Override
	public void close() {
		_closed = true;
		try {
			_listener.close();
		} catch (Throwable t) {
			// ignore
		}
		try {
			_baseListener.close();
		} catch (Throwable t) {
			// ignore
		}
	}
}
