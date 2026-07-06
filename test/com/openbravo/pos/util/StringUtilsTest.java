package com.openbravo.pos.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class StringUtilsTest {

    @Test
    public void byte2hexThenHex2byteRoundTrips() {
        byte[] original = new byte[] {0, 1, 15, 16, (byte) 127, (byte) 128, (byte) 255};

        String hex = StringUtils.byte2hex(original);
        byte[] restored = StringUtils.hex2byte(hex);

        assertArrayEquals(original, restored);
    }

    @Test
    public void isNumberAcceptsOnlyDigits() {
        assertTrue(StringUtils.isNumber("1234567890"));
        assertFalse(StringUtils.isNumber(""));
        assertFalse(StringUtils.isNumber(null));
        assertFalse(StringUtils.isNumber("12a34"));
        assertFalse(StringUtils.isNumber("-123"));
    }
}
