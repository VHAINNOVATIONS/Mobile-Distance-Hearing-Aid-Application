// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;

import org.yaler.proxy.ProxyHelper;
import org.yaler.util.StreamHelper;

public class YalerServerSocket implements Closeable {
    private final String _host;
	private final int _port;
	private final String _id;
	private final Proxy _proxy;

	private volatile boolean _closed;
	private volatile Socket _listener;

	public YalerServerSocket(String host, int port, String id) {
		this(host, port, id, ProxyHelper.selectProxy(host, port));
	}

	public YalerServerSocket(String host, int port, String id, Proxy proxy) {
		_host = host;
		_port = port;
		_id = id;
		_proxy = proxy;
	}

	public YalerSocketFactory getYalerSocketFactory() {
		return YalerSocketFactory.getInstance();
	}

	public InetSocketAddress findLocation(InputStream stream) throws IOException {
		return StreamHelper.findLocation(stream, "http", 80);
	}

        public Socket accept() throws IOException {
            return accept(null);
        }

        public Socket accept(AcceptCallback acceptCallback) throws IOException {
		if (_closed) {
			throw new SocketException("YalerServerSocket is closed");
		}
        if (acceptCallback != null) {
            acceptCallback.statusChanged(AcceptCallbackState.Undefined);
        }
		try {
		    String host = _host;
	        int port = _port;
	        Socket result = null;
	        boolean acceptable = false;
	        int[] x = new int[3];
			do {
				_listener = getYalerSocketFactory().createSocket(_proxy, host, port, 5000);
				if (!_closed) {
					_listener.setSoTimeout(75000);
					_listener.setTcpNoDelay(true);
					result = _listener;
					InputStream i = result.getInputStream();
					OutputStream o = result.getOutputStream();
		            do {
                        o.write((
		            		"POST /" + _id + " HTTP/1.1\r\n" +
		            		"Upgrade: PTTH/1.0\r\n" +
                            "Connection: Upgrade\r\n" +
                            "Host: " + host + "\r\n\r\n").getBytes());
		            	for (int j = 0; j != 12; j++) {
							x[j % 3] = i.read();
						}
                        if ((x[0] == '3') && (x[1] == '0') && (x[2] == '7')) {
		            		InetSocketAddress address = findLocation(i);
		            		host = address.getHostName();
		            		port = address.getPort();
		            	}
		            	acceptable = StreamHelper.find(i, "\r\n\r\n");
                        if ((acceptCallback != null) &&
                            acceptable && ((x[0] == '2') && (x[1] == '0') && (x[2] == '4')))
                        {
                            acceptCallback.statusChanged(AcceptCallbackState.Accessible);
                        }
		            } while (acceptable && ((x[0] == '2') && (x[1] == '0') && (x[2] == '4')));
		            if (acceptable && (x[0] == '1') && (x[1] == '0') &&(x[2] == '1')) {
                        result.setSoTimeout(0);
                        if (acceptCallback != null) {
                            acceptCallback.statusChanged(AcceptCallbackState.Connected);
                        }
		            } else {
		            	result.close();
		            	result = null;
		            }
	            }
			} while (acceptable && ((x[0] == '3') && (x[1] == '0') && (x[2] == '7')));
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
	}
}
