package com.krk.poc.crypto.controller;

import com.krk.poc.crypto.service.TransactionDataService;
import com.krk.poc.crypto.service.ARQCService;
import com.krk.poc.crypto.util.TlvUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/acquirer")
public class GatewayForwarderController {

    private final TransactionDataService txnDataService;
    private final ARQCService arqcService;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/emvtx")
    public ResponseEntity<?> generateAndForward(@RequestBody Map<String, Object> request) throws Exception {
        String pan = (String) request.get("pan");
        String field55 = (String) request.get("field55");
        String unpredictableNumber = (String) request.get("unpredictableNumber");

        // Step 1: Parse field55 into map
        Map<String, String> tlvMap = TlvUtils.parseTlv(field55);

        // Step 2: Extract ATC (9F36) and build txnData
        String atc = tlvMap.get("9F36");
        if (atc == null || atc.length() < 4) {
            throw new IllegalArgumentException("Missing/invalid 9F36 (ATC) in field55");
        }
        String txnData = txnDataService.createTxnData(field55);
        log.info("transaction data: {}", txnData);

        // Step 3: Generate ARQC
        String arqc = arqcService.generateARQC(pan, atc, unpredictableNumber, txnData);
        log.info("Generated ARQC: {}", arqc);

        // Step 4: Inject 9F26 = ARQC
        tlvMap.put("9F26", arqc);
        String updatedField55 = TlvUtils.serializeTlv(tlvMap);
        log.info("Updated field55 with ARQC: {}", updatedField55);

        // Step 5: Build final payload
        request.put("field55", updatedField55);  // replace original with updated

        // Step 6: Forward to payment-gateway
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> gatewayResponse;
        try {
            gatewayResponse = restTemplate.exchange(
                  "http://localhost:8221/gateway/process",
                  HttpMethod.POST,
                  entity,
                  String.class
            );
        } catch (RestClientException ex) {
            log.error("Error forwarding to payment-gateway: {}", ex.getMessage());
            // Return 502 with useful context but keep ARQC + updated field55
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                  "arqc", arqc,
                  "updatedField55", updatedField55,
                  "error", "Failed to reach payment-gateway at http://localhost:8221/gateway/process",
                  "detail", ex.getMessage()
            ));
        }
        log.info("Updated field 55 {}", updatedField55);
        return ResponseEntity.ok(Map.of(
              "arqc", arqc,
//              "updatedField55", updatedField55,
              "gatewayResponse", gatewayResponse.getBody()
        ));
    }
}
