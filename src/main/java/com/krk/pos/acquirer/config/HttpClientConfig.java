package com.krk.pos.acquirer.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

   @Bean
   public CloseableHttpClient apacheHttpClient(
         @Value("${http.client.maxTotal:100}") int maxTotal,
         @Value("${http.client.maxPerRoute:20}") int maxPerRoute,
         @Value("${http.client.connectTimeoutSec:5}") int connectTimeoutSec,
         @Value("${http.client.responseTimeoutSec:10}") int responseTimeoutSec,
         @Value("${http.client.socketSoTimeoutSec:10}") int socketSoTimeoutSec,
         @Value("${http.client.evictIdleSec:30}") int evictIdleSec
   ) {
      var cm = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(maxTotal)
            .setMaxConnPerRoute(maxPerRoute)
            .setDefaultSocketConfig(SocketConfig.custom()
                  .setSoTimeout(Timeout.ofSeconds(socketSoTimeoutSec)) // socket read timeout
                  .build())
            .build();

      return HttpClients.custom()
            .setConnectionManager(cm)
            .setDefaultRequestConfig(RequestConfig.custom()
                  .setConnectTimeout(Timeout.ofSeconds(connectTimeoutSec))
                  .setResponseTimeout(Timeout.ofSeconds(responseTimeoutSec))
                  .build())
            .evictExpiredConnections()
            .evictIdleConnections(TimeValue.ofSeconds(evictIdleSec))
            .build();
   }

   @Bean
   public ClientHttpRequestFactory clientHttpRequestFactory(CloseableHttpClient httpClient) {
      return new HttpComponentsClientHttpRequestFactory(httpClient);
   }

   /** RestClient pre-configured to talk to payment-gateway (base URL from application.yaml). */
   @Bean(name = "paymentGatewayRestClient")
   public RestClient paymentGatewayRestClient(
         ClientHttpRequestFactory factory,
         @Value("${payment.gateway.baseUrl}") String baseUrl
   ) {
      return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .build();
   }
}
