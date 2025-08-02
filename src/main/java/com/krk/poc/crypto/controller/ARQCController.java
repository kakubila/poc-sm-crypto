package com.krk.poc.crypto.contoller;

import com.krk.poc.crypto.service.ARQCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/emva")
public class ARQCController {

    @Autowired
    private ARQCService arqcService;

    @PostMapping("/generate")
    public Map<String, String> generate(@RequestBody Map<String, String> request) throws Exception {
        String arqc = arqcService.generateARQC(
                request.get("pan"),
                request.get("atc"),
                request.get("unpredictableNumber"),
                request.get("transactionData")
        );
        return Map.of("arqc", arqc);
    }

    @PostMapping("/validate")
    public Map<String, Boolean> validate(@RequestBody Map<String, String> request) throws Exception {
        boolean valid = arqcService.validateARQC(
                request.get("arqc"),
                request.get("pan"),
                request.get("atc"),
                request.get("unpredictableNumber"),
                request.get("transactionData")
        );
        return Map.of("valid", valid);
    }
}
