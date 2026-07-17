//    uniCenta oPOS  - Touch Friendly Point Of Sale
//    Copyright (c) 2009-2014 uniCenta
//    http://www.unicenta.com
//
//    This file is part of uniCenta oPOS
//
//    uniCenta oPOS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//   uniCenta oPOS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with uniCenta oPOS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.util;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encripta/desencripta valores de configuracion (claves de BD, ERP, pasarelas de pago)
 * a partir de una passphrase derivada de un dato conocido (p.ej. "cypherkey" + usuario).
 * Como la passphrase es predecible, esto sigue siendo ofuscacion y no un secreto
 * criptografico real (igual que ClaveCifrador en ecopos-sri-connector) - pero
 * usa AES-GCM (autenticado, con IV aleatorio) en vez del DESEDE/ECB original,
 * que reutilizaba el mismo bloque cifrado para bloques de texto plano identicos
 * y no protegia la integridad del dato.
 *
 * Los valores ya cifrados con el esquema anterior se siguen pudiendo desencriptar
 * (prefijo "AESGCM:" ausente = formato legado), para no romper claves ya guardadas
 * en instalaciones existentes. encrypt() siempre produce el formato nuevo.
 *
 * @author JG uniCenta
 */
public class AltEncrypter {

    private static final String PREFIJO_AESGCM = "AESGCM:";
    private static final int TAM_IV = 12;
    private static final int TAM_TAG_BITS = 128;

    private final SecretKeySpec claveAESGCM;

    private Cipher cipherDecryptLegado;

    /** Creates a new instance of Encrypter
     * @param passPhrase */
    public AltEncrypter(String passPhrase) {

        claveAESGCM = derivarClaveAESGCM(passPhrase);

        try {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(passPhrase.getBytes("UTF8"));
            KeyGenerator kGen = KeyGenerator.getInstance("DESEDE");
            kGen.init(168, sr);
            Key key = kGen.generateKey();

            cipherDecryptLegado = Cipher.getInstance("DESEDE/ECB/PKCS5Padding");
            cipherDecryptLegado.init(Cipher.DECRYPT_MODE, key);
        } catch (UnsupportedEncodingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
        }
    }

    private static SecretKeySpec derivarClaveAESGCM(String passPhrase) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return new SecretKeySpec(sha256.digest(passPhrase.getBytes("UTF8")), "AES");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     *
     * @param str
     * @return
     */
    public String encrypt(String str) {
        try {
            byte[] iv = new byte[TAM_IV];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, claveAESGCM, new GCMParameterSpec(TAM_TAG_BITS, iv));
            byte[] textoCifrado = cipher.doFinal(str.getBytes("UTF8"));

            byte[] ivMasCifrado = new byte[iv.length + textoCifrado.length];
            System.arraycopy(iv, 0, ivMasCifrado, 0, iv.length);
            System.arraycopy(textoCifrado, 0, ivMasCifrado, iv.length, textoCifrado.length);

            return PREFIJO_AESGCM + StringUtils.byte2hex(ivMasCifrado);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
        }
        return null;
    }

    /**
     *
     * @param str
     * @return
     */
    public String decrypt(String str) {
        if (str != null && str.startsWith(PREFIJO_AESGCM)) {
            return decryptAESGCM(str.substring(PREFIJO_AESGCM.length()));
        }
        return decryptLegado(str);
    }

    private String decryptAESGCM(String hexIvMasCifrado) {
        try {
            byte[] ivMasCifrado = StringUtils.hex2byte(hexIvMasCifrado);
            byte[] iv = Arrays.copyOfRange(ivMasCifrado, 0, TAM_IV);
            byte[] textoCifrado = Arrays.copyOfRange(ivMasCifrado, TAM_IV, ivMasCifrado.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, claveAESGCM, new GCMParameterSpec(TAM_TAG_BITS, iv));
            return new String(cipher.doFinal(textoCifrado), "UTF8");
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
        }
        return null;
    }

    private String decryptLegado(String str) {
        try {
            return new String(cipherDecryptLegado.doFinal(StringUtils.hex2byte(str)), "UTF8");
        } catch (UnsupportedEncodingException | BadPaddingException | IllegalBlockSizeException e) {
        }
        return null;
    }
}
