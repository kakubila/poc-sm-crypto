package com.krk.pos.acquirer.controller;

import com.krk.pos.acquirer.client.PaymentGatewayClient;
import com.krk.pos.acquirer.service.PinBlockService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pin")
public class PinController {

   private final PinBlockService svc;
   private final PaymentGatewayClient gateway;

   public PinController(PinBlockService svc, PaymentGatewayClient gateway) {
      this.svc = svc;
      this.gateway = gateway;
   }

   @PostMapping("/encrypt")
   public Map<String, Object> encrypt(@RequestBody Map<String, String> req) {
      return svc.encryptIso0ViaHsm(
            req.get("pan"),
            req.get("pin"),
            req.get("keySlot"),
            req.get("keyName"),
            req.get("zpkHex")
      );
   }

   /**
    * Mirror ARQC flow for PIN:
    *  1) Build ISO-0 EPB locally (terminal/ICC behavior)
    *  2) Forward to payment-gateway /gateway/process for issuer-side verification (EMPT path)
    */
   @PostMapping("/verify")
   public Map<String, Object> verify(@RequestBody Map<String, Object> req) {
      // seed from POS
      String pan     = (String) req.get("pan");
      String pin     = (String) req.get("pin");
      String keySlot = (String) req.get("keySlot");
      String keyName = (String) req.get("keyName");
      String zpkHex  = (String) req.get("zpkHex");

      // 1) build EPB locally using existing service (no new service api)
      @SuppressWarnings("unchecked")
      Map<String, Object> epin = (Map<String, Object>) (Map<?, ?>) svc.encryptIso0ViaHsm(
            pan, pin, keySlot, keyName, zpkHex
      );
      String pinBlock = (String) epin.get("pinBlock");

      // 2) construct gateway request
      Map<String, Object> zpkRef = new HashMap<>();
      if (notBlank(keySlot))      zpkRef.put("keySlot", keySlot);
      else if (notBlank(keyName)) zpkRef.put("keyName", keyName);
      else if (notBlank(zpkHex))  zpkRef.put("keyValue", zpkHex);

      Map<String, Object> gwReq = new HashMap<>();
      gwReq.put("pan", pan);
      gwReq.put("pinBlock", pinBlock);
      gwReq.put("pinFormatIn", "ISO-0");
      gwReq.put("zpkRef", zpkRef);

      // optional translateTo passthrough (either nested object or flat pinFormatOut)
      Object translateTo = req.get("translateTo");
      if (translateTo instanceof Map) {
         gwReq.put("translateTo", translateTo);
      } else if (req.containsKey("pinFormatOut")) {
         Map<String, Object> t = new HashMap<>();
         t.put("pinFormatOut", String.valueOf(req.get("pinFormatOut")));
         gwReq.put("translateTo", t);
      }

      // 3) forward to payment-gateway (which forwards to payment-switch)
      return gateway.process(gwReq);
   }

   private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }
}

