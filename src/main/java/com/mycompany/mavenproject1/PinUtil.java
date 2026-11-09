package com.mycompany.mavenproject1;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Utilitas setup satu kali: menghasilkan hash PIN yang bisa disimpan di kolom kasir.pin_hash. */
public final class PinUtil {
    private static final int ITERATIONS = 120000;
    private static final int KEY_BITS = 256;

    private PinUtil() {}

    public static String hash(char[] pin) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(pin, salt, ITERATIONS, KEY_BITS);
        byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
        spec.clearPassword();
        return Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(result);
    }

    public static void main(String[] args) throws Exception {
        char[] pin;
        // NetBeans exec-maven-plugin dapat meneruskan satu argumen kosong ketika
        // exec.appArgs disetel kosong. Kondisi itu tetap harus membuka dialog.
        if (args.length == 1 && args[0] != null && !args[0].isEmpty()) {
            pin = args[0].toCharArray();
        } else {
            javax.swing.JPasswordField field = new javax.swing.JPasswordField(12);
            int choice = javax.swing.JOptionPane.showConfirmDialog(null, field,
                    "Masukkan PIN baru (minimal 4 digit)",
                    javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
            if (choice != javax.swing.JOptionPane.OK_OPTION) return;
            pin = field.getPassword();
        }
        if (pin.length < 4) {
            java.util.Arrays.fill(pin, '\0');
            throw new IllegalArgumentException("PIN minimal 4 digit.");
        }
        try {
            String encoded = hash(pin);
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                System.out.println(encoded);
            } else {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(encoded), null);
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Hash PIN disalin ke clipboard. Tempelkan ke kolom pin_hash di database.");
            }
        } finally {
            java.util.Arrays.fill(pin, '\0');
        }
    }
}
