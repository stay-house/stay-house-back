package com.example.stay_house_back.controller;

import com.example.stay_house_back.dto.eligibility.EligibilityRequest;
import com.example.stay_house_back.dto.eligibility.EligibilityResponse;
import com.example.stay_house_back.service.EligibilityFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final EligibilityFilterService eligibilityFilterService;

    @MessageMapping("/check")
    public void check(EligibilityRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.debug("WebSocket eligibility check - sessionId={}", sessionId);

        EligibilityResponse response = eligibilityFilterService.filter(request);

        messagingTemplate.convertAndSend("/topic/eligibility/" + sessionId, response);
    }
}
