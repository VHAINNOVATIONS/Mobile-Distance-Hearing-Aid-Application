// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.yaler.proxy.ntlm.NTLMSchemeFactory;
import org.yaler.util.HttpHelper;
import org.yaler.util.MalformedHttpResponseException;

public final class ProxyClient {
	private ProxyClient() {}

	private static final boolean isSchemeChar(char c) {
		return (32 < c) && (c < 127) && (c != '(') && (c != ')') && (c != '<') && (c != '>') && (c != '@')
				&& (c != ',') && (c != ';') && (c != ':') && (c != '\\') && (c != '"') && (c != '/') && (c != '[')
				&& (c != ']') && (c != '?') && (c != '=') && (c != '{') && (c != '}');
	}

	private static final String scheme(String challenge) {
		String result = null;
		int j = 0, n = challenge.length();
		while (j != n && !isSchemeChar(challenge.charAt(j))) {
			j++;
		}
		if (j != n) {
			int i = j;
			do {
				j++;
			} while (j != n && isSchemeChar(challenge.charAt(j)));
			result = challenge.substring(i, j);
		}
		return result;
	}

	public static final Socket connectProxy(Proxy proxy, String host, int port, int timeout) throws IOException, MalformedHttpResponseException {
		InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
		Socket s = null;
		final String basic = "Basic";
		final String digest = "Digest";
		final String ntlm = "NTLM";
		String token = null, challenge = null;
		boolean authorizing = false;
		AuthScheme scheme = null;
		do {
			InetAddress a = proxyAddress.getAddress();
			if (a == null) {
				a = InetAddress.getByName(proxyAddress.getHostName());
			}
			s = new Socket(a, proxyAddress.getPort());
            s.setSoTimeout(timeout);
			OutputStream outstream = s.getOutputStream();
			InputStream instream = s.getInputStream();
			if (scheme != null) {
				// select AuthScheme
				Header authHeader = new BasicHeader("Proxy-Authenticate", challenge);
				try {
					scheme.processChallenge(authHeader);
				} catch (MalformedChallengeException e) {
					throw new ProxyAuthenticationException(proxy, e);
				}
				// fetch credentials
				Credentials cred = null;
				if (proxy instanceof AuthenticatedProxy) {
					cred = ((AuthenticatedProxy)proxy).getCredentials();
				} else {
					InetSocketAddress isAddress = (InetSocketAddress) proxy.address();
					PasswordAuthentication auth = Authenticator.requestPasswordAuthentication(
							isAddress.getAddress(),
							isAddress.getPort(),
							"http", "", scheme.getSchemeName());
					if (auth == null) {
						throw new ProxyAuthenticationException("credentials could no be fetched from Authenticator", proxy);
					}
					cred = new UsernamePasswordCredentials(auth.getUserName(), new String(auth.getPassword()));
				}
				if (cred == null) {
					throw new ProxyAuthenticationException("credentials could not be fetched", proxy);
				}
				// authenticate
				try {
					BasicHttpRequest request = new BasicHttpRequest("CONNECT", host+':'+port);
					request.addHeader("Proxy-Authenticate", challenge);
// TODO? (ContextAwareAuthScheme probably not available on Android)
//					if (scheme instanceof ContextAwareAuthScheme) {
//						authHeader = ((ContextAwareAuthScheme) scheme).authenticate(cred, request, null);
//	                } else {
	                	authHeader = scheme.authenticate(cred, request);
//	                }
				} catch(AuthenticationException e) {
					throw new ProxyAuthenticationException(proxy, e);
				}
				if (authHeader != null) {
					token = authHeader.getValue();
				} else {
					throw new ProxyAuthenticationException(proxy);
				}
			}
			StringBuilder builder = new StringBuilder();
			builder.append("CONNECT ").append(host).append(':').append(port).append(" HTTP/1.1\r\n");
			builder.append("Host: ").append(proxyAddress.getHostName() + "\r\n");
			if (token != null) {
				builder.append("Proxy-Authorization: ").append(token).append("\r\n");
			}
			builder.append("\r\n");
			outstream.write(builder.toString().getBytes());
			int statusCode = HttpHelper.receiveHttpStatusLineStatusCode(instream);
			Map<String,String> headers = HttpHelper.receiveHttpHeaders(instream);
			int contentLength = HttpHelper.getContentLength(headers);
			HttpHelper.skipBody(instream, contentLength);
			if (statusCode == HttpHelper.HTTP_OK) {
				authorizing = false;
			} else if (statusCode == HttpHelper.HTTP_PROXY_AUTHENTICATION_REQUIRED) {
				String responseAuthChallenge = headers.get("Proxy-Authenticate");
				if (responseAuthChallenge != null) {
					String responseAuthType = ProxyClient.scheme(responseAuthChallenge);
					if (ntlm.equalsIgnoreCase(responseAuthType)) {
						if (challenge == null) {
							scheme = new NTLMSchemeFactory().newInstance(null);
							challenge = responseAuthChallenge;
							authorizing = true;
						} else {
							throw new ProxyAuthenticationException(proxy);
						}
					} else if (digest.equalsIgnoreCase(responseAuthType)) {
						if (challenge == null) {
							scheme = new DigestSchemeFactory().newInstance(null);
							challenge = responseAuthChallenge;
							authorizing = true;
						} else {
							throw new ProxyAuthenticationException(proxy);
						}
					} else if (basic.equalsIgnoreCase(responseAuthType)) {
						if (challenge == null) {
							scheme = new BasicSchemeFactory().newInstance(null);
							challenge = responseAuthChallenge;
							authorizing = true;
						} else {
							throw new ProxyAuthenticationException(proxy);
						}
					} else {
						throw new ProxyAuthenticationException("unknown scheme " + responseAuthType, proxy);
					}
				} else {
					throw new ProxyAuthenticationException("no challenge provided", proxy);
				}
			} else {
				throw new ProxyAuthenticationException("unmanaged http status code " + statusCode, proxy);
			}
			if (authorizing) {
				try {
					s.close();
				} catch(IOException e) {
					// ignore
				}
			}
		} while (authorizing);
		return s;
	}

}
