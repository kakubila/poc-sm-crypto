package com.krk.pos.acquirer.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class PaymentGatewayClient {

   private final RestClient rest;

   public PaymentGatewayClient(@Qualifier("paymentGatewayRestClient") RestClient rest) {
      this.rest = rest;
   }

   /** Send to payment-gateway; it forwards to payment-switch. */
   @SuppressWarnings("unchecked")
   public Map<String, Object> process(Map<String, Object> body) {
      return rest.post()
            .uri("/gateway/process")  // correct endpoint
            .body(body)
            .retrieve()
            .body(Map.class);
   }
}

