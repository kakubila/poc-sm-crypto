package com.krk.poc.crypto.controller;

import com.krk.poc.crypto.service.TransactionDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/parse")
public class TxnDataController {

    @Autowired
    private TransactionDataService transactionDataService;

    @PostMapping("/field55")
    public Map<String, String> generateTxnData(@RequestBody Map<String, Object> request) throws Exception {
        String field55 = (String) request.get("field55");
        String txnData = transactionDataService.createTxnData(field55);
        return Map.of("txnData", txnData);
    }
}
