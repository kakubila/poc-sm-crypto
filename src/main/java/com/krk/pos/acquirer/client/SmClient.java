package com.krk.pos.acquirer.client;

import com.krk.pos.acquirer.client.dto.EpinRequestDto;
import com.krk.pos.acquirer.client.dto.EpinResponseDto;

public interface SmClient {
   EpinResponseDto generatePin(EpinRequestDto req);
   EpinResponseDto translatePin(EpinRequestDto req);
}
