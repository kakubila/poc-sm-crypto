package com.krk.poc.crypto.util;

import java.util.*;

public class TlvUtils {
    // Simple parser: returns a map of tag -> value (hex)
    public static Map<String, String> parseTlv(String tlv) {
        Map<String, String> map = new LinkedHashMap<>();
        int i = 0;
        while (i < tlv.length()) {
            // Tag: 2-4 hex digits (EMV tags can be 1-2 bytes)
            String tag = tlv.substring(i, i + 4);
            i += 4;
            // Length: next 2 hex digits
            int len = Integer.parseInt(tlv.substring(i, i + 2), 16);
            i += 2;
            // Value: length * 2 hex digits
            String value = tlv.substring(i, i + len * 2);
            i += len * 2;
            map.put(tag, value);
        }
        return map;
    }

    public static String getTagValue(String tlv, String tag) {
        Map<String, String> map = parseTlv(tlv);
        return map.getOrDefault(tag, null);
    }

    public static String updateTagValue(String tlv, String tag, String newValueHex) {
        Map<String, String> map = parseTlv(tlv);
        map.put(tag, newValueHex);
        return serializeTlv(map);
    }

    public static String serializeTlv(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            String tag = e.getKey();
            String value = e.getValue();
            int len = value.length() / 2;
            sb.append(tag);
            sb.append(String.format("%02X", len));
            sb.append(value);
        }
        return sb.toString();
    }
}
