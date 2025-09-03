package com.krk.pos.acquirer.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class TlvUtils {

    private static int b(String s, int i) { // read 1 byte (2 hex chars)
        return Integer.parseInt(s.substring(i, i + 2), 16);
    }

    // Read EMV tag (1–3 bytes)
    private static String readTag(String tlv, int[] idx) {
        int i = idx[0];
        if (i + 2 > tlv.length()) throw new IllegalArgumentException("Incomplete tag at " + i);
        StringBuilder tag = new StringBuilder(tlv.substring(i, i + 2));
        int first = b(tlv, i);
        i += 2;

        if ( (first & 0x1F) == 0x1F ) { // more tag bytes follow
            boolean more = true; int left = 2; // up to total 3 bytes
            while (more && left-- > 0) {
                if (i + 2 > tlv.length()) throw new IllegalArgumentException("Incomplete multi-byte tag at " + i);
                int tb = b(tlv, i);
                tag.append(tlv, i, i + 2);
                i += 2;
                more = (tb & 0x80) == 0x80; // bit8 set -> more bytes
            }
        }
        idx[0] = i;
        return tag.toString();
    }

    // Read EMV length (short or long form)
    private static int readLen(String tlv, int[] idx) {
        int i = idx[0];
        if (i + 2 > tlv.length()) throw new IllegalArgumentException("Missing length at " + i);
        int first = b(tlv, i);
        i += 2;

        if ( (first & 0x80) == 0 ) { // short form
            idx[0] = i;
            return first;
        }
        int numBytes = first & 0x7F;
        if (numBytes < 1 || numBytes > 3) throw new IllegalArgumentException("Unsupported length-of-length " + numBytes + " at " + (i - 2));
        if (i + numBytes * 2 > tlv.length()) throw new IllegalArgumentException("Incomplete long length at " + i);

        int len = 0;
        for (int n = 0; n < numBytes; n++) {
            len = (len << 8) | b(tlv, i);
            i += 2;
        }
        idx[0] = i;
        return len;
    }

    public static Map<String, String> parseTlv(String tlv) {
        if (tlv == null) throw new IllegalArgumentException("TLV is null");
        tlv = tlv.trim();
        if ((tlv.length() & 1) == 1) throw new IllegalArgumentException("Odd number of hex chars");

        Map<String, String> out = new LinkedHashMap<>();
        int[] idx = {0};
        try {
            while (idx[0] < tlv.length()) {
                String tag = readTag(tlv, idx);
                int len = readLen(tlv, idx);
                int i = idx[0];
                int end = i + len * 2;
                if (end > tlv.length()) throw new IllegalArgumentException("Not enough data for tag " + tag + " len=" + len + " at " + i);
                String val = tlv.substring(i, end);
                idx[0] = end;
                out.put(tag, val);
            }
        } catch (Exception e) {
            int at = Math.min(idx[0], tlv.length());
            String ctx = tlv.substring(Math.max(0, at - 8), Math.min(tlv.length(), at + 8));
            throw new RuntimeException("TLV parse error at " + at + " near '" + ctx + "': " + e.getMessage(), e);
        }
        return out;
    }

    public static String serializeTlv(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            String tag = e.getKey();
            String val = e.getValue() == null ? "" : e.getValue();
            int len = val.length() / 2;

            sb.append(tag);
            if (len <= 0x7F) {
                sb.append(String.format("%02X", len));
            } else if (len <= 0xFF) {
                sb.append("81").append(String.format("%02X", len));
            } else if (len <= 0xFFFF) {
                sb.append("82").append(String.format("%04X", len));
            } else {
                sb.append("83").append(String.format("%06X", len));
            }
            sb.append(val);
        }
        return sb.toString();
    }

    public static String getTagValue(String tlv, String tag) {
        return parseTlv(tlv).get(tag);
    }

    public static String updateTagValue(String tlv, String tag, String newValueHex) {
        Map<String, String> m = parseTlv(tlv);
        m.put(tag, newValueHex == null ? "" : newValueHex);
        return serializeTlv(m);
    }
}