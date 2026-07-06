package com.openbravo.pos.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class LuhnAlgorithmTest {

    @Test
    public void acceptsKnownValidCardNumbers() {
        assertTrue(LuhnAlgorithm.checkCC("4111111111111111")); // Visa
        assertTrue(LuhnAlgorithm.checkCC("5500000000000004")); // MasterCard
        assertTrue(LuhnAlgorithm.checkCC("340000000000009"));  // AMEX
        assertTrue(LuhnAlgorithm.checkCC("30000000000004"));   // Diners
        assertTrue(LuhnAlgorithm.checkCC("6011000000000004")); // Discover
    }

    @Test
    public void rejectsCardNumberWithBadChecksum() {
        assertFalse(LuhnAlgorithm.checkCC("4111111111111112"));
    }

    @Test
    public void rejectsNonNumericInput() {
        assertFalse(LuhnAlgorithm.checkCC("not-a-card"));
        assertFalse(LuhnAlgorithm.checkCC(""));
    }
}
