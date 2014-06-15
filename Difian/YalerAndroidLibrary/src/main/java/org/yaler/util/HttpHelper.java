// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class HttpHelper {
	private HttpHelper() {}

	public static final int HTTP_SWITCHING_PROTOCOLS = 101;
	public static final int HTTP_OK = 200;
	public static final int HTTP_NO_CONTENT = 204;
	public static final int HTTP_TEMPORARY_REDIRECT = 307;
	public static final int HTTP_PROXY_AUTHENTICATION_REQUIRED = 407;

	public static final int receiveHttpStatusLineStatusCode(InputStream stream) throws IOException, MalformedHttpResponseException {
		int statusCode = -1;
		String line = StreamHelper.readLine(stream);
		if (line != null && line.length() >= 12) {
			try {
				statusCode = Integer.parseInt(line.substring(9, 12));
			} catch(NumberFormatException e) {
				throw new MalformedHttpResponseException("illegal status code");
			}
		} else {
			throw new MalformedHttpResponseException("line is no status line");
		}
		return statusCode;
	}

	public static final Map<String, String> receiveHttpHeaders(InputStream stream) throws IOException {
		Map<String,String> headers = new HashMap<String,String>();
		String line = null, name = null, value = null;
		line = StreamHelper.readLine(stream);
		while (!line.isEmpty()) { // reached the end of the header
			int i = line.indexOf(':');
			if (i != -1) {
				name = line.substring(0, i);
				value = line.substring(i+1).trim();
				line = StreamHelper.readLine(stream);
				while (!line.isEmpty() && (line.charAt(0) == '\t' || line.charAt(0) == ' ')) {
					value = value + ' ' + line.trim();
					line = StreamHelper.readLine(stream);
				}
				headers.put(name, value);
			}
		}
		return headers;
	}

	public static final int getContentLength(Map<String,String> headers) throws MalformedHttpResponseException {
		int contentLength = 0;
		String value = headers.get("Content-Length");
		if (value != null) {
			try {
				contentLength = Integer.parseInt(value);
			} catch(NumberFormatException e) {
				throw new MalformedHttpResponseException("illegal value for header Content-Length");
			}
		}
		return contentLength;
	}

	public static final void skipBody(InputStream stream, int length) throws IOException {
		byte[] buffer = new byte[256];
        int c = 0;
        if (length < 0) {
            // consume until EOF
            while ((c = stream.read(buffer)) != -1) {
            	// skip
            }
        } else {
            // consume no more than length
            long remaining = length;
            while (remaining > 0 && c != -1) {
                c = stream.read(buffer, 0, (int)Math.min(buffer.length, remaining));
                if (c != -1) {
                    remaining -= c;
                }
            }
        }
	}

}
