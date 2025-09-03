package com.krk.pos.acquirer.util;

import org.apache.commons.codec.binary.Hex;
import java.util.LinkedHashMap;
import java.util.Map;

public class EMVField55Parser {
    public static Map<String, String> parseTLV(String field55) throws Exception {
        Map<String, String> tags = new LinkedHashMap<>();
        byte[] data = Hex.decodeHex(field55);
        int index = 0;

        while (index < data.length) {
            // Parse Tag
            StringBuilder tag = new StringBuilder();
            tag.append(String.format("%02X", data[index++]));
            if ((data[index - 1] & 0x1F) == 0x1F) { // Multi-byte tag
                tag.append(String.format("%02X", data[index++]));
            }
            
            // Parse Length
            int length = data[index++] & 0xFF;
            if (length > 0x80) {
                int numBytes = length - 0x80;
                length = 0;
                for (int i = 0; i < numBytes; i++) {
                    length = (length << 8) + (data[index++] & 0xFF);
                }
            }
            
            // Parse Value
            byte[] valueBytes = new byte[length];
            System.arraycopy(data, index, valueBytes, 0, length);
            index += length;

            tags.put(tag.toString().toUpperCase(), Hex.encodeHexString(valueBytes).toUpperCase());
        }
        return tags;
    }
}