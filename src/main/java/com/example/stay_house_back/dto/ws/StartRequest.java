package com.example.stay_house_back.dto.ws;

import com.example.stay_house_back.dto.eligibility.EligibilityRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * /app/start 메시지. 클라이언트가 생성한 UUID(planRequestId)와
 * 자격 조건 요청을 함께 받는다.
 * 서버는 /topic/plan/{planRequestId} 로 응답을 보낸다.
 */
@Getter
@NoArgsConstructor
public class StartRequest {
    private String planRequestId;
    private EligibilityRequest eligibilityRequest;
}
