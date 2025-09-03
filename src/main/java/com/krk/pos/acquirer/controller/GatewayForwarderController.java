package com.krk.pos.acquirer.controller;

import com.krk.pos.acquirer.service.TransactionDataService;
import com.krk.pos.acquirer.service.ARQCService;
import com.krk.pos.acquirer.util.TlvUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/acquirer")
public class GatewayForwarderController {

    private final TransactionDataService txnDataService;
    private final ARQCService arqcService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${payment.gateway.baseUrl:http://localhost:8221}")
    private String gatewayBaseUrl;

    /**
     * Accepts the current acquirer JSON (with field55 missing 9F26),
     * computes ARQC, injects 9F26 into field55, and forwards the NEW canonical
     * JSON body to payment-gateway.
     */
    @PostMapping("/emvtx")
    public ResponseEntity<?> generateAndForward(@RequestBody Map<String, Object> request) throws Exception {
        // 1) Extract required inputs from incoming request (current shape)
        final String pan = asString(request.get("pan"));
        final String expiryDate = asString(request.get("expiryDate"));
        final String field55 = asString(request.get("field55"));
        final String unpredictableNumber = asString(request.get("unpredictableNumber"));
        final String transactionCurrencyCode = asString(request.get("transactionCurrencyCode"));
        final String terminalCountryCode = asString(request.get("terminalCountryCode"));
        final Integer amount = asInt(request.get("amount"));

        // 2) Build transaction data + extract ATC for ARQC
        final String txnData = txnDataService.createTxnData(field55);
        final String atc = TlvUtils.getTagValue(field55, "9F36");
        if (atc == null || atc.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                  .body(Map.of(
                        "error", "Missing tag 9F36 (ATC) in field55",
                        "trace", "GatewayForwarderController:ATC"
                  ));
        }

        // 3) Generate ARQC and inject 9F26 into field55
        final String arqc = arqcService.generateARQC(pan, atc, unpredictableNumber, txnData);
        final String updatedField55 = TlvUtils.updateTagValue(field55, "9F26", arqc);

        // assert 9F26 exists, is 8 bytes (16 hex chars), and matches computed ARQC
        final String f9f26 = TlvUtils.getTagValue(updatedField55, "9F26");
        if (f9f26 == null || f9f26.length() != 16 || !f9f26.equalsIgnoreCase(arqc)) {
            return ResponseEntity.unprocessableEntity().body(Map.of(
                  "code", "E.TLV.9F26",
                  "message", "9F26 missing or inconsistent with computed ARQC",
                  "expectedArqc", arqc
            ));
        }

        // (nice-to-have) also ensure UN consistency with 9F37
        final String f9f37 = TlvUtils.getTagValue(updatedField55, "9F37");
        if (f9f37 == null || !f9f37.equalsIgnoreCase(unpredictableNumber)) {
            return ResponseEntity.unprocessableEntity().body(Map.of(
                  "code", "E.TLV.9F37",
                  "message", "9F37 in field55 does not match unpredictableNumber",
                  "expected9F37", unpredictableNumber
            ));
        }

        // 4) Construct NEW canonical payload expected by payment-gateway
        Map<String, Object> forwardBody = new LinkedHashMap<>();
        forwardBody.put("pan", pan);
        forwardBody.put("expiryDate", expiryDate);
        forwardBody.put("processingCode", "000000");
        forwardBody.put("amount", amount);
        forwardBody.put("transmissionDateTime", "0901102233");
        forwardBody.put("stan", "123456");
        forwardBody.put("posEntryMode", "101");
        forwardBody.put("panSequenceNumber", "001");
        forwardBody.put("posConditionCode", "00");
        forwardBody.put("track2", "5081470082639564D30032210123456789");
        forwardBody.put("terminalId", "TERM0001");
        forwardBody.put("merchantId", "MRC000000000001");
        forwardBody.put("transactionCurrencyCode", transactionCurrencyCode);
        forwardBody.put("terminalCountryCode", terminalCountryCode);
        forwardBody.put("pinBlockHex", "FFFFFFFFFFFFFFFF");
        forwardBody.put("securityControlInfo", "0000000000000000");
        forwardBody.put("field55", updatedField55); // includes 9F26 (ARQC)
        forwardBody.put("unpredictableNumber", unpredictableNumber);

        // 5) Forward to payment-gateway
        String url = gatewayBaseUrl + "/gateway/process"; // adjust if your gateway path differs
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(forwardBody, headers);
        log.info("Forwarding to gateway at {} with body: {}", url, forwardBody);

        ResponseEntity<?> gatewayResponse;
        try {
            gatewayResponse = restTemplate.postForEntity(url, entity, Map.class);
        } catch (RestClientException ex) {
            log.error("Failed to reach payment-gateway at {}", url, ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                  "arqc", arqc,
                  "updatedField55", updatedField55,
                  "error", "Failed to reach payment-gateway",
                  "detail", ex.getMessage()
            ));
        }

        log.info("Forwarded to gateway: {}", url);
        return ResponseEntity.ok(Map.of(
              "arqc", arqc,
              "gatewayResponse", gatewayResponse.getBody()
        ));
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); }
        catch (NumberFormatException e) { return null; }
    }
}
