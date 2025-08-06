package com.krk.poc.crypto.controller;

import com.krk.poc.crypto.service.TransactionApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/txn")
public class TransactionOrchestrationController {

    @Autowired
    private TransactionApprovalService transactionApprovalService;

    @PostMapping("/approve")
    public ResponseEntity<?> approveTransaction(@RequestBody Map<String, Object> req) throws Exception {
        Map<String, Object> result = transactionApprovalService.approveTransaction(req);
        return ResponseEntity.ok(result);
    }
}