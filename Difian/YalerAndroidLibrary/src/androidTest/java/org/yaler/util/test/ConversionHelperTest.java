// Copyright (c) 2014, Yaler GmbH, Switzerland
// All rights reserved

package org.yaler.util.test;

import junit.framework.TestCase;

import org.yaler.util.ConversionHelper;

public class ConversionHelperTest extends TestCase {
	private static void testIntToByteArrayCase(int value, byte[] expected) {
		byte[] actual = ConversionHelper.intToByteArray(value);
		for (int i = 0; i < 4; i++) {
			assertEquals(expected[i], actual[i]);
		}
	}

	private static void testByteArrayToIntCase(byte[] value, int expected) {
		int actual = ConversionHelper.byteArrayToInt(value, 0);
		assertEquals(expected, actual);
	}

	public final void testIntToByteArray() {
		testIntToByteArrayCase(255, new byte[] { (byte) 0xFF, 0x00, 0x00, 0x00 });
		testIntToByteArrayCase(0x94C7, new byte[] { (byte) 0xC7, (byte) 0x94, 0x00, 0x00 });
		testIntToByteArrayCase(33, new byte[] { 33, 0, 0, 0 });
		testIntToByteArrayCase(0xFFFFFFFF, new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });
	}

	public final void testByteArrayToInt () {
		testByteArrayToIntCase(new byte[] { (byte) 0xFF, 0x00, 0x00, 0x00 }, 255);
		testByteArrayToIntCase(new byte[] { (byte) 0xC7, (byte) 0x94, 0x00, 0x00 }, 0x94C7);
		testByteArrayToIntCase(new byte[] { 33, 0, 0, 0 }, 33);
		testByteArrayToIntCase(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, 0xFFFFFFFF);
	}
}
