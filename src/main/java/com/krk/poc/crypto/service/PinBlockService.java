package com.krk.poc.crypto.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class PinBlockService {

   @Value("${crypto.zpk.keyValue:}")
   private String defaultZpkHex;

   /** Existing: local ISO-0 EPB generation (kept as you had it) */
   public Map<String,Object> encryptIso0ViaHsm(String pan, String pin,
                                               String keySlot, String keyName, String zpkHex) {
      String zpk = chooseZpk(zpkHex);
      byte[] clear = buildIso0ClearPinBlock(pan, pin);
      byte[] key24 = normalize3DesKey(hexToBytes(zpk));
      byte[] enc   = encrypt3DesEcbNoPadding(key24, clear);

      Map<String,Object> out = new HashMap<>();
      out.put("pinBlock", bytesToHex(enc));
      out.put("format", "ISO-0");
      out.put("hsmCmd", "LOCAL-EPIN");
      out.put("keyRef", keyRef(keySlot, keyName, zpkHex));
      return out;
   }

   /** NEW: verify EPB by recomputing locally and comparing */
   public Map<String,Object> verifyIso0Local(String pan, String pin, String pinBlockHex,
                                             String keySlot, String keyName, String zpkHex) {
      String zpk = chooseZpk(zpkHex);

      // recompute EPB under same PAN/ZPK
      byte[] clear = buildIso0ClearPinBlock(pan, pin);
      byte[] key24 = normalize3DesKey(hexToBytes(zpk));
      String expected = bytesToHex(encrypt3DesEcbNoPadding(key24, clear));

      String provided = normalizeHex(pinBlockHex);
      boolean match = expected.equalsIgnoreCase(provided);

      Map<String,Object> out = new HashMap<>();
      out.put("result", match ? "MATCH" : "MISMATCH");
      out.put("pinFormat", "ISO-0");
      // (optional) include keyRef or computed/received blocks for debugging
      // out.put("keyRef", keyRef(keySlot, keyName, zpkHex));
      // out.put("computedPinBlock", expected);
      return out;
   }

   // ---- helpers (same as before) ----
   private String chooseZpk(String zpkHexOverride) {
      String z = (zpkHexOverride != null && !zpkHexOverride.isBlank()) ? zpkHexOverride : defaultZpkHex;
      if (z == null || z.isBlank())
         throw new IllegalArgumentException("ZPK hex not provided (req.zpkHex or crypto.zpk.keyValue)");
      return z;
   }

   private static String keyRef(String keySlot, String keyName, String zpkHex) {
      if (keySlot != null && !keySlot.isBlank()) return "BD" + keySlot;
      if (keyName != null && !keyName.isBlank()) return "#" + keyName;
      if (zpkHex  != null && !zpkHex.isBlank())  return "HEX";
      return null;
   }

   /** ISO-0 clear block (PIN-field XOR PAN-field) */
   public static byte[] buildIso0ClearPinBlock(String pan, String pin) {
      if (pin == null || !pin.matches("\\d{4,12}")) {
         throw new IllegalArgumentException("PIN must be 4–12 digits");
      }
      String pan12 = derivePan12(pan);
      String pinFieldHex = "0" + Integer.toHexString(pin.length()).toUpperCase(Locale.ROOT)
            + pin + "F".repeat(14 - pin.length());
      String panFieldHex = "0000" + pan12;
      byte[] pinField = hexToBytes(pinFieldHex);
      byte[] panField = hexToBytes(panFieldHex);
      byte[] out = new byte[8];
      for (int i = 0; i < 8; i++) out[i] = (byte) (pinField[i] ^ panField[i]);
      return out;
   }

   public static String derivePan12(String pan) {
      if (pan == null) throw new IllegalArgumentException("PAN is required");
      String d = pan.replaceAll("\\D", "");
      if (d.length() < 13) throw new IllegalArgumentException("PAN too short for PAN12");
      return d.substring(d.length() - 13, d.length() - 1);
   }

   public static byte[] normalize3DesKey(byte[] key) {
      if (key.length == 16) {
         byte[] out = new byte[24];
         System.arraycopy(key, 0, out, 0, 16);
         System.arraycopy(key, 0, out, 16, 8); // K1|K2|K1
         return out;
      } else if (key.length == 24) {
         return key;
      }
      throw new IllegalArgumentException("ZPK must be 16 or 24 bytes (hex length 32 or 48)");
   }

   public static byte[] encrypt3DesEcbNoPadding(byte[] key24, byte[] block8) {
      try {
         Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
         cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key24, "DESede"));
         return cipher.doFinal(block8);
      } catch (Exception e) {
         throw new IllegalStateException("3DES encryption failed", e);
      }
   }

   private static String normalizeHex(String hex) {
      if (hex == null) return "";
      return hex.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
   }

   public static byte[] hexToBytes(String hex) {
      String s = hex.replaceAll("\\s+", "");
      if ((s.length() & 1) != 0) throw new IllegalArgumentException("Hex length must be even");
      byte[] out = new byte[s.length() / 2];
      for (int i = 0; i < s.length(); i += 2) {
         out[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
      }
      return out;
   }

   public static String bytesToHex(byte[] bytes) {
      StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) sb.append(String.format("%02X", b));
      return sb.toString();
   }
}
