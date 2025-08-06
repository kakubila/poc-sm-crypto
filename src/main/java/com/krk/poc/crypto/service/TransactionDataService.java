package com.krk.poc.crypto.service;

import com.krk.poc.crypto.util.EMVField55Parser;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class TransactionDataService {

    public String createTxnData(String field55) throws Exception {
        Map<String, String> tags = EMVField55Parser.parseTLV(field55);

        String cvr = extractCVR(tags.getOrDefault("9F10", ""));

        return new StringBuilder()
            .append(tags.getOrDefault("9F02", ""))  // Amount Authorized
            .append(tags.getOrDefault("9F03", ""))  // Amount, Other
            .append(tags.getOrDefault("5F2A", ""))  // Transaction Currency Code
            .append(tags.getOrDefault("95", ""))    // TVR
            .append(tags.getOrDefault("9F1A", ""))  // Terminal Country Code
            .append(tags.getOrDefault("9A", ""))    // Transaction Date
            .append(tags.getOrDefault("9F27", ""))  // Cryptogram Info Data
            .append(tags.getOrDefault("9F37", ""))  // Unpredictable Number
            .append(tags.getOrDefault("82", ""))    // Application Interchange Profile
            .append(tags.getOrDefault("9F36", ""))  // ATC
            .append(cvr)                            // CVR (from 9F10)
            .toString();
    }

    private String extractCVR(String tag9F10) {
        // Extract CVR (first 6 bytes / 12 hex chars after the first 4 chars of 9F10)
        if (tag9F10.length() >= 16) {
            return tag9F10.substring(4, 16);
        }
        return "";
    }
}
