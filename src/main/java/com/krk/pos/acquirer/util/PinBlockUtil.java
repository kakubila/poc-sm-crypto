package com.krk.pos.acquirer.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

/**
 * ISO-9564 PIN block helpers (Format 0) + 3DES encryption under ZPK.
 * No javax.* APIs; pure JCE.
 */
public final class PinBlockUtil {
    private PinBlockUtil() {}

    /** Build ISO-9564 Format 0 clear PIN block (8 bytes) from PIN digits and PAN. */
    public static byte[] buildIso0ClearBlock(String pin, String pan) {
        if (pin == null || pin.length() < 4 || pin.length() > 12)
            throw new IllegalArgumentException("PIN must be 4..12 digits");
        for (int i = 0; i < pin.length(); i++)
            if (!Character.isDigit(pin.charAt(i)))
                throw new IllegalArgumentException("PIN must be numeric");

        String pan12 = extractPan12(pan);

        // PIN field: '0' + length nibble + digits + 'F' pad to 16 nibbles
        StringBuilder pinField = new StringBuilder();
        pinField.append('0').append(Integer.toHexString(pin.length()).toUpperCase());
        pinField.append(pin);
        while (pinField.length() < 16) pinField.append('F');

        // PAN field: 0000 + rightmost 12 digits of PAN excluding check digit; left-pad to 16 nibbles
        StringBuilder panField = new StringBuilder("0000").append(pan12);
        while (panField.length() < 16) panField.insert(0, '0');

        byte[] pinBytes = hexToBytes(pinField.toString());
        byte[] panBytes = hexToBytes(panField.toString());

        byte[] clear = new byte[8];
        for (int i = 0; i < 8; i++) clear[i] = (byte) (pinBytes[i] ^ panBytes[i]);
        return clear;
    }

    /** Encrypt PIN block (3DES EDE) under a double- or triple-length ZPK (hex 32 or 48). */
    public static String encryptUnderZpk(byte[] clear8, String zpkHex) {
        byte[] key = normalize3DesKey(hexToBytes(notBlank(zpkHex, "zpkHex")));
        try {
            SecretKey sk = new SecretKeySpec(key, "DESede");
            Cipher c = Cipher.getInstance("DESede/ECB/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, sk);
            byte[] ct = c.doFinal(clear8);
            return bytesToHex(ct);
        } catch (Exception e) {
            throw new IllegalStateException("ZPK encryption failed", e);
        }
    }

    /** Convenience: build ISO-0 then encrypt under ZPK. */
    public static String encryptIso0(String pin, String pan, String zpkHex) {
        return encryptUnderZpk(buildIso0ClearBlock(pin, pan), zpkHex);
    }

    /** KCV = 3DES-ECB(0x00..00)[0..2] */
    public static String calcKcv3(String zpkHex) {
        byte[] key = normalize3DesKey(hexToBytes(notBlank(zpkHex, "zpkHex")));
        try {
            SecretKey sk = new SecretKeySpec(key, "DESede");
            Cipher c = Cipher.getInstance("DESede/ECB/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, sk);
            byte[] out = c.doFinal(new byte[8]);
            return bytesToHex(Arrays.copyOf(out, 3));
        } catch (Exception e) {
            throw new IllegalStateException("KCV calc failed", e);
        }
    }

    // --- helpers ---

    /** Extract 12 rightmost digits of PAN excluding the check digit (ISO-0). */
    public static String extractPan12(String pan) {
        if (pan == null) throw new IllegalArgumentException("PAN required");
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < pan.length(); i++) {
            char c = pan.charAt(i);
            if (c >= '0' && c <= '9') digits.append(c);
        }
        if (digits.length() < 13)
            throw new IllegalArgumentException("PAN must have at least 13 digits");
        String withoutCheck = digits.substring(0, digits.length() - 1);
        String right12 = withoutCheck.substring(Math.max(0, withoutCheck.length() - 12));
        return right12;
    }

    private static String notBlank(String s, String name) {
        if (s == null || s.trim().isEmpty())
            throw new IllegalArgumentException(name + " is required");
        return s.trim();
    }

    /** Normalize to 24-byte 3DES key material: if 16-byte (double-length), repeat K1 to make K1|K2|K1. */
    private static byte[] normalize3DesKey(byte[] key) {
        if (key.length == 16) {
            byte[] out = new byte[24];
            System.arraycopy(key, 0, out, 0, 16);
            System.arraycopy(key, 0, out, 16, 8);
            return out;
        } else if (key.length == 24) {
            return key;
        } else {
            throw new IllegalArgumentException("ZPK must be 16 or 24 bytes (hex 32 or 48 chars)");
        }
    }

    public static byte[] hexToBytes(String hex) {
        String h = hex.replaceAll("\\s", "");
        if ((h.length() & 1) != 0) throw new IllegalArgumentException("Odd hex length");
        byte[] b = new byte[h.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int hi = Character.digit(h.charAt(i * 2), 16);
            int lo = Character.digit(h.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("Non-hex character at " + (i*2));
            b[i] = (byte) ((hi << 4) | lo);
        }
        return b;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte x : bytes) sb.append(String.format("%02X", x));
        return sb.toString();
    }
}
