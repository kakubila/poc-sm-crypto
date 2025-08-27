package com.krk.poc.crypto.client;

import com.krk.poc.crypto.client.dto.EpinRequestDto;
import com.krk.poc.crypto.client.dto.EpinResponseDto;

public interface SmClient {
   EpinResponseDto generatePin(EpinRequestDto req);
   EpinResponseDto translatePin(EpinRequestDto req);
}
