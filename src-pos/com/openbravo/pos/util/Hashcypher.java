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

import java.awt.Component;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import com.openbravo.beans.JPasswordDialog;
import com.openbravo.pos.forms.AppLocal;

/**
 * Formato de contrasenas guardadas: prefijo + datos ("empty:", "plain:...",
 * "sha1:...", "pbkdf2:iteraciones:saltHex:hashHex"). Cada prefijo nuevo se
 * agrego sin romper la lectura de los anteriores (mismo patron que ya existia
 * entre "plain:" y "sha1:"), para que ninguna cuenta existente quede bloqueada.
 * "pbkdf2:" es el formato actual: PBKDF2WithHmacSHA256 con sal aleatoria por
 * usuario e iteraciones altas, en vez del SHA-1 sin sal anterior (vulnerable a
 * tablas arcoiris y fuerza bruta). Las cuentas existentes migran solas la
 * proxima vez que el usuario cambia su contrasena (hashString() ya se llama
 * ahi), igual que paso historicamente con la migracion de "plain:" a "sha1:".
 *
 * @author JG uniCenta
 */
public class Hashcypher {

    private static final int PBKDF2_ITERACIONES = 210000;
    private static final int PBKDF2_TAM_SAL_BYTES = 16;
    private static final int PBKDF2_TAM_HASH_BITS = 256;

    /** Creates a new instance of Hashcypher */
    public Hashcypher() {
    }

    /**
     *
     * @param sPassword
     * @param sHashPassword
     * @return
     */
    public static boolean authenticate(String sPassword, String sHashPassword) {
        if (sHashPassword == null || sHashPassword.equals("") || sHashPassword.startsWith("empty:")) {
            return sPassword == null || sPassword.equals("");
        } else if (sHashPassword.startsWith("pbkdf2:")) {
            return authenticatePbkdf2(sPassword, sHashPassword);
        } else if (sHashPassword.startsWith("sha1:")) {
            return sHashPassword.equals(hashStringSha1Legado(sPassword));
        } else if (sHashPassword.startsWith("plain:")) {
            return sHashPassword.equals("plain:" + sPassword);
        } else {
            return sHashPassword.equals(sPassword);
        }
    }

    /**
     *
     * @param sPassword
     * @return
     */
    public static String hashString(String sPassword) {

        if (sPassword == null || sPassword.equals("")) {
            return "empty:";
        } else {
            try {
                byte[] sal = new byte[PBKDF2_TAM_SAL_BYTES];
                SecureRandom.getInstanceStrong().nextBytes(sal);
                byte[] hash = pbkdf2(sPassword.toCharArray(), sal, PBKDF2_ITERACIONES, PBKDF2_TAM_HASH_BITS);
                return "pbkdf2:" + PBKDF2_ITERACIONES + ":" + StringUtils.byte2hex(sal) + ":" + StringUtils.byte2hex(hash);
            } catch (GeneralSecurityException e) {
                return "plain:" + sPassword;
            }
        }
    }

    private static String hashStringSha1Legado(String sPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(sPassword.getBytes("UTF-8"));
            byte[] res = md.digest();
            return "sha1:" + StringUtils.byte2hex(res);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return "plain:" + sPassword;
        }
    }

    private static boolean authenticatePbkdf2(String sPassword, String sHashPassword) {
        try {
            String[] partes = sHashPassword.split(":");
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = StringUtils.hex2byte(partes[2]);
            byte[] hashEsperado = StringUtils.hex2byte(partes[3]);
            byte[] hashCalculado = pbkdf2(sPassword.toCharArray(), sal, iteraciones, hashEsperado.length * 8);
            return constantTimeEquals(hashCalculado, hashEsperado);
        } catch (GeneralSecurityException | RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] sal, int iteraciones, int tamHashBits) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, sal, iteraciones, tamHashBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int resultado = 0;
        for (int i = 0; i < a.length; i++) {
            resultado |= a[i] ^ b[i];
        }
        return resultado == 0;
    }
    
    /**
     *
     * @param parent
     * @return
     */
    public static String changePassword(Component parent) {
        // Show the changePassword dialogs but do not check the old password
        
        String sPassword = JPasswordDialog.showEditPassword(parent,                 
                AppLocal.getIntString("Label.Password"), 
                AppLocal.getIntString("label.passwordnew"),
                new ImageIcon(Hashcypher.class.getResource("/com/openbravo/images/password.png")));
        if (sPassword != null) {
            String sPassword2 = JPasswordDialog.showEditPassword(parent,                 
                    AppLocal.getIntString("Label.Password"), 
                    AppLocal.getIntString("label.passwordrepeat"),
                    new ImageIcon(Hashcypher.class.getResource("/com/openbravo/images/password.png")));
            if (sPassword2 != null) {
                if (sPassword.equals(sPassword2)) {
                    return  Hashcypher.hashString(sPassword);
                } else {
                    JOptionPane.showMessageDialog(parent, AppLocal.getIntString("message.changepassworddistinct"), AppLocal.getIntString("message.title"), JOptionPane.WARNING_MESSAGE);
                }
            }
        }   
        
        return null;
    }

    /**
     *
     * @param parent
     * @param sOldPassword
     * @return
     */
    public static String changePassword(Component parent, String sOldPassword) {
        
        String sPassword = JPasswordDialog.showEditPassword(parent,                 
                AppLocal.getIntString("Label.Password"), 
                AppLocal.getIntString("label.passwordold"),
                new ImageIcon(Hashcypher.class.getResource("/com/openbravo/images/password.png")));
        if (sPassword != null) {
            if (Hashcypher.authenticate(sPassword, sOldPassword)) {
                return changePassword(parent);               
            } else {
                JOptionPane.showMessageDialog(parent, AppLocal.getIntString("message.BadPassword"), AppLocal.getIntString("message.title"), JOptionPane.WARNING_MESSAGE);
           }
        }
        return null;
    }
}
