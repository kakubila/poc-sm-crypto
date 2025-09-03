package com.krk.pos.acquirer.client;

import com.krk.pos.acquirer.client.dto.EpinRequestDto;
import com.krk.pos.acquirer.client.dto.EpinResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "crypto.pin.remote", havingValue = "false", matchIfMissing = true)
public class LocalSmClient implements SmClient {

   private final String defaultZpkHex;

   public LocalSmClient(@Value("${crypto.zpk.keyValue:}") String defaultZpkHex) {
      this.defaultZpkHex = defaultZpkHex == null ? "" : defaultZpkHex.trim();
   }

   @Override
   public EpinResponseDto generatePin(EpinRequestDto req) {
      String pan = req.getPan();
      String pin = req.getPin();
      String zpkHex = (req.getZpkHex() != null && !req.getZpkHex().isBlank())
            ? req.getZpkHex()
            : defaultZpkHex;
      if (zpkHex == null || zpkHex.isBlank())
         throw new IllegalArgumentException("ZPK hex not provided (req.zpkHex or crypto.zpk.keyValue)");

      byte[] clear = buildIso0ClearBlock(pin, pan);
      byte[] key24 = normalize3DesKey(hexToBytes(zpkHex));
      byte[] enc   = encrypt3DesEcbNoPadding(key24, clear);

      EpinResponseDto out = new EpinResponseDto();
      out.setPinBlock(bytesToHex(enc));
      out.setFormat("ISO-0");
      out.setHsmCmd("LOCAL-EPIN");
      out.setKeyRef(keyRef(req));
      return out;
   }

   @Override
   public EpinResponseDto translatePin(EpinRequestDto req) {
      throw new UnsupportedOperationException("Terminal/ICC does not translate/verify PIN");
   }

   // ----- helpers (self-contained to avoid extra deps) -----
   private static String keyRef(EpinRequestDto r) {
      if (r.getKeySlot() != null && !r.getKeySlot().isBlank()) return "BD" + r.getKeySlot();
      if (r.getKeyName() != null && !r.getKeyName().isBlank()) return "#" + r.getKeyName();
      if (r.getZpkHex()  != null && !r.getZpkHex().isBlank())  return "HEX";
      return null;
   }
   private static byte[] buildIso0ClearBlock(String pin, String pan) {
      if (pin == null || !pin.matches("\\d{4,12}")) throw new IllegalArgumentException("PIN must be 4–12 digits");
      String pinFieldHex = "0" + Integer.toHexString(pin.length()).toUpperCase(Locale.ROOT)
            + pin + "F".repeat(14 - pin.length());
      String pan12 = derivePan12(pan);
      String panFieldHex = "0000" + pan12;
      byte[] pinField = hexToBytes(pinFieldHex);
      byte[] panField = hexToBytes(panFieldHex);
      byte[] out = new byte[8];
      for (int i = 0; i < 8; i++) out[i] = (byte) (pinField[i] ^ panField[i]);
      return out;
   }
   private static String derivePan12(String pan) {
      if (pan == null) throw new IllegalArgumentException("PAN is required");
      String d = pan.replaceAll("\\D", "");
      if (d.length() < 13) throw new IllegalArgumentException("PAN too short for PAN12");
      return d.substring(d.length() - 13, d.length() - 1);
   }
   private static byte[] normalize3DesKey(byte[] key) {
      if (key.length == 16) {
         byte[] out = new byte[24];
         System.arraycopy(key, 0, out, 0, 16);
         System.arraycopy(key, 0, out, 16, 8); // K1|K2|K1
         return out;
      } else if (key.length == 24) {
         return key;
      }
      throw new IllegalArgumentException("ZPK must be 16 or 24 bytes (hex length 32/48)");
   }
   private static byte[] encrypt3DesEcbNoPadding(byte[] key24, byte[] block8) {
      try {
         Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
         cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, new SecretKeySpec(key24, "DESede"));
         return cipher.doFinal(block8);
      } catch (Exception e) {
         throw new IllegalStateException("3DES encryption failed", e);
      }
   }
   private static byte[] hexToBytes(String hex) {
      String s = hex.replaceAll("\\s+", "");
      if ((s.length() & 1) != 0) throw new IllegalArgumentException("Hex length must be even");
      byte[] out = new byte[s.length() / 2];
      for (int i = 0; i < s.length(); i += 2)
         out[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
      return out;
   }
   private static String bytesToHex(byte[] bytes) {
      StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) sb.append(String.format("%02X", b));
      return sb.toString();
   }
}

