// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.UUID;

import junit.framework.TestCase;

import org.yaler.YalerSSLServerSocket;
import org.yaler.YalerStreamingClient;
import org.yaler.YalerStreamingServerSocket;

import android.util.Log;

public class YalerStreamingTest extends TestCase {
	private static final String TAG = "YalerStreamingTest";

	private String _relayProtocol;
	private String _unreachableRelayHost;

	byte[] _testBuffer;
	String _relayHost;
	String _relayDomain;
	YalerStreamingServerSocket _server;
	YalerStreamingServerSocket _unreachableServer;

	@Override
	protected void setUp () {
		_testBuffer = new byte[1024];
		for (int i=0; i < _testBuffer.length; i++) {
			_testBuffer[i] = (byte) (Math.random() * 255);
		}
		_relayHost = "try.yaler.net";
		_unreachableRelayHost = "try.yaler.invalid"; // see http://tools.ietf.org/html/rfc2606
		_relayDomain = String.format("difian-%s", UUID.randomUUID());
		Log.i(TAG, _relayDomain);
		_relayProtocol = "https";
		_server = new YalerStreamingServerSocket(new YalerSSLServerSocket(
				_relayHost, 443, _relayDomain));
		_unreachableServer = new YalerStreamingServerSocket(new YalerSSLServerSocket(
				_unreachableRelayHost, 443, _relayDomain));
	}

	public final void testAcceptFail () {
		try {
			_unreachableServer.accept();
			fail();
		} catch (IOException e) {
			// expected
		}
	}

	public final void testConnection () {
        assertNotNull(_relayProtocol);
		assertNotNull(_server);
		Thread acceptThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Socket socket = _server.accept();
					OutputStream stream = socket.getOutputStream();
					stream.write(_testBuffer);
					stream.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		try {
			acceptThread.start();
			Thread.sleep(1337);
			// TODO: while (socket == null)
			Socket socket = YalerStreamingClient.connectSSLSocket(
				_relayHost, 443, _relayDomain, null);
			assertNotNull(socket);
			InputStream in = socket.getInputStream();
			int ch = in.read();
			int i = 0;
			while (ch != -1) {
				assertTrue((byte) ch == _testBuffer[i]);
				ch = in.read();
				i++;
			}
	        in.close();
	        socket.close();
	        acceptThread.join();
		} catch (MalformedURLException e) {
			fail();
		} catch (IOException e) {
			fail();
		} catch (InterruptedException e) {
			fail();
		}
	}

	public final void testClose () {
		assertNotNull(_relayProtocol);
		assertNotNull(_server);
		Thread connectThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1337);
					Socket socket = YalerStreamingClient.connectSSLSocket(
						_relayHost, 443,
						_relayDomain, null);
					InputStream in = socket.getInputStream();
					in.read(); // blocking
			        in.close();
			        socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		connectThread.start();
		try {
			Socket socket = _server.accept();
			assertNotNull(socket);
			assertTrue(!socket.isClosed());
			_server.close();
			assertTrue(!socket.isClosed());
			socket.close();
			assertTrue(socket.isClosed());
			connectThread.join();
		} catch (IOException e) {
			fail();
		} catch (InterruptedException e) {
			fail();
		}
	}

	@Override
	protected void tearDown() throws Exception {
		_server.close();
		_unreachableServer.close();
	}
}
