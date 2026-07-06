package com.openbravo.pos.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class AltEncrypterTest {

    @Test
    public void encryptThenDecryptReturnsOriginalValue() {
        AltEncrypter cipher = new AltEncrypter("cypherkeytestuser");
        String original = "s3cr3t-p@ssw0rd";

        String encrypted = cipher.encrypt(original);
        String decrypted = cipher.decrypt(encrypted);

        assertNotNull(encrypted);
        assertFalse(original.equals(encrypted));
        assertEquals(original, decrypted);
    }

    @Test
    public void differentPassphrasesProduceDifferentCiphertext() {
        String original = "same-input";
        String encryptedA = new AltEncrypter("cypherkeyA").encrypt(original);
        String encryptedB = new AltEncrypter("cypherkeyB").encrypt(original);

        assertFalse(encryptedA.equals(encryptedB));
    }

    @Test
    public void emptyStringRoundTrips() {
        AltEncrypter cipher = new AltEncrypter("cypherkeyempty");
        String encrypted = cipher.encrypt("");
        assertEquals("", cipher.decrypt(encrypted));
    }
}
