package com.krk.pos.acquirer.client;

import com.krk.pos.acquirer.client.dto.EpinRequestDto;
import com.krk.pos.acquirer.client.dto.EpinResponseDto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "crypto.pin.remote", havingValue = "true")
public class SmInterfaceClient implements SmClient {

   private final RestTemplate rest;
   private final String baseUrl;
   private final String epinPath;
   private final String emptPath;

   public SmInterfaceClient(
         @Value("${sm.interface.baseUrl:http://sm-interface:8342}") String baseUrl,
         @Value("${sm.interface.path.epin:/api/hsm/epin}") String epinPath,
         @Value("${sm.interface.path.empt:/api/hsm/empt}") String emptPath
   ) {
      this.rest = new RestTemplate();
      this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl;
      this.epinPath = epinPath.startsWith("/") ? epinPath : ("/" + epinPath);
      this.emptPath = emptPath.startsWith("/") ? emptPath : ("/" + emptPath);
   }

   @Override
   public EpinResponseDto generatePin(EpinRequestDto req) {
      String url = baseUrl + epinPath;
      HttpHeaders h = new HttpHeaders();
      h.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<EpinRequestDto> entity = new HttpEntity<>(req, h);
      return rest.postForObject(url, entity, EpinResponseDto.class);
   }

   @Override
   public EpinResponseDto translatePin(EpinRequestDto req) {
      String url = baseUrl + emptPath;
      HttpHeaders h = new HttpHeaders();
      h.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<EpinRequestDto> entity = new HttpEntity<>(req, h);
      return rest.postForObject(url, entity, EpinResponseDto.class);
   }
}