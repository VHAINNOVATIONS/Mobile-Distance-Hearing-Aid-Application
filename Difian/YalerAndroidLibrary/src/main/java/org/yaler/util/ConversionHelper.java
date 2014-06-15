// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler.util;

public final class ConversionHelper {
	ConversionHelper () {}

	public static byte[] intToByteArray (int value) {
		return new byte[] {
			(byte) value,
			(byte) (value >>> 8),
			(byte) (value >>> 16),
			(byte) (value >>> 24),

		};
	}

	public static int byteArrayToInt (byte[] b, int off) {
		return
			(b[off] & 0xFF) |
			((b[off + 1] & 0xFF) << 8) |
			((b[off + 2] & 0xFF) << 16) |
			((b[off + 3] & 0xFF) << 24);
	}
}
