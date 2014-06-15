// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public final class KeepAliveSocket {
	private static final class KeepAliveSocketInputStream extends InputStream {
		private static final byte[] EMPTY_BUFFER = new byte[0];

		private final KeepAliveSocket _socket;
		private final InputStream _stream;
		private final Object _lock = new Object();

		private int _remaining; // guarded by _lock

        private KeepAliveSocketInputStream (KeepAliveSocket socket) throws IOException {
			_socket = socket;
			_stream = socket.getInnerSocket().getInputStream();
			Thread thread = new Thread(new Runnable() {
				@Override
                public void run () {
					runKeepAlive();
				}
			});
			thread.setDaemon(true);
			thread.start();
		}

		private void runKeepAlive () {
			try {
				synchronized (_lock) {
					while (_remaining >= 0) {
						while (_remaining > 0) {
							_lock.wait();
						}
						doRead(EMPTY_BUFFER, 0, 0);
					}
				}
			} catch (Throwable e) {
				_socket.notifyKeepAliveFailedListener();
			}
		}

		public int doRead(byte[] buffer, int off, int len) throws IOException {
			int result = len == 0 ? 0 : -1;
			if (_remaining == 0) {
				byte[] lengthBuffer = new byte[4];
				int headerRead;
				do {
					int headerPos = 0;
					do {
						headerRead = _stream.read(
							lengthBuffer, headerPos, lengthBuffer.length - headerPos);
						assert headerRead != 0;
						headerPos += Math.max(0, headerRead);
					} while ((headerRead > 0) && (headerPos != lengthBuffer.length));
					if (headerPos == lengthBuffer.length) {
						_remaining = ConversionHelper.byteArrayToInt(lengthBuffer, 0);
					} else {
						_remaining = -1;
					}
				} while (_remaining == 0);
			}
			if (_remaining > 0) {
				// Some clients of this library rely on package semantics,
				// even though stream abstraction doesn't guarantee it.
				int bytesToRead = Math.min(len, this._remaining);
				int bufferRead;
				int bufferPos = off;
				do {
					bufferRead = _stream.read(
						buffer, bufferPos, bytesToRead - (bufferPos - off));
					bufferPos += Math.max(0, bufferRead);
				} while ((bufferRead > 0) && (bufferPos - off != bytesToRead));
				result = bufferPos - off == bytesToRead ? bytesToRead : -1;
				_remaining -= bufferPos - off;
			}
			return result;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			synchronized (_lock) {
				try {
					return doRead(b, off, len);
				} finally {
					if (_remaining <= 0) {
						_lock.notify();
					}
				}
			}
		}

		@Override
		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		@Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			int read = read(b, 0, 1);
			assert read == -1 || read == 1;
			return read == -1 ? -1 : (b[0] & 0xFF);
		}

		@Override
		public int available() {
			synchronized (_lock) {
				return _remaining;
			}
		}

		@Override
		public void close () throws IOException {
			_stream.close();
		}
	}

	private static final class KeepAliveSocketOutputStream extends OutputStream {

		private static final byte[] EMPTY_BUFFER = new byte[0];

		private final KeepAliveSocket _socket;
		private final OutputStream _stream;
		private final long _keepAliveInterval;
		private final Object _lock = new Object();

		private KeepAliveSocketOutputStream (KeepAliveSocket socket) throws IOException {
			_socket = socket;
			_stream = socket.getInnerSocket().getOutputStream();
			_keepAliveInterval = socket.getKeepAliveInterval();
			Thread thread = new Thread(new Runnable() {
				@Override
                public void run () {
					runKeepAlive();
				}
			});
			thread.setDaemon(true);
			thread.start();
		}

		private void runKeepAlive () {
			try {
				while (true) {
					write(EMPTY_BUFFER);
					Thread.sleep(_keepAliveInterval);
				}
			} catch (Throwable e) {
				_socket.notifyKeepAliveFailedListener();
			}
		}

		@Override
		public void write (byte[] buffer, int offset, int count) throws IOException {
			synchronized (_lock) {
				byte[] lengthBuffer = ConversionHelper.intToByteArray(count);
				_stream.write(lengthBuffer, 0, lengthBuffer.length);
				if (count > 0) {
					_stream.write(buffer, offset, count);
				}
			}
		}

		@Override
		public void write (byte[] buffer) throws IOException {
			write(buffer, 0, buffer.length);
		}

		@Override
		public void write (int b) throws IOException {
			synchronized (_lock) {
				_stream.write(1);
				_stream.write(0);
				_stream.write(0);
				_stream.write(0);
				_stream.write(b);
			}
		}

		@Override
		public void flush () throws IOException {
			synchronized (_lock) {
				_stream.flush();
			}
		}

		@Override
		public void close () throws IOException {
			synchronized (_lock) {
				_stream.close();
			}
		}
	}

	public interface KeepAliveFailedListener {
		void keepAliveFailed (Object sender);
	}

	private final Socket _innerSocket;
	private final long _keepAliveInterval;
	private final InputStream _inputStream;
	private final OutputStream _outputStream;
	private final KeepAliveFailedListener _keepAliveFailedListener;

	private volatile boolean _closed;

	public KeepAliveSocket (
		Socket socket, long keepAliveInterval,
		KeepAliveFailedListener keepAliveFailedListener)
		throws IOException
	{
		_keepAliveInterval = keepAliveInterval;
		_innerSocket = socket;
		_keepAliveFailedListener = keepAliveFailedListener;
		_inputStream = new KeepAliveSocketInputStream(this);
		_outputStream = new KeepAliveSocketOutputStream(this);
	}

	private Socket getInnerSocket () {
		return _innerSocket;
	}

	private long getKeepAliveInterval () {
		return _keepAliveInterval;
	}

	private void notifyKeepAliveFailedListener () {
		if ((_keepAliveFailedListener != null) && !_closed) {
			_keepAliveFailedListener.keepAliveFailed(this);
		}
	}

	public boolean isClosed() {
		return _closed;
	}

	public InputStream getInputStream() {
		return _inputStream;
	}

	public OutputStream getOutputStream() {
		return _outputStream;
	}

	public void close () throws IOException {
		_closed = true;
		try {
			_inputStream.close();
		} finally {
			try {
				_outputStream.close();
			} finally {
				_innerSocket.close();
			}
		}
	}
}
