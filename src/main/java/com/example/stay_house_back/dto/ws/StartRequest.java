package com.example.stay_house_back.dto.ws;

import com.example.stay_house_back.dto.eligibility.EligibilityRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * /app/start 메시지. 클라이언트가 생성한 UUID(planRequestId)와
 * 자격 조건 요청을 함께 받는다.
 * 서버는 /topic/plan/{planRequestId} 로 응답을 보낸다.
 *
 * 우대금리 질문이 있으면 서버가 NEED_MORE_INFO 로 응답하고, 클라이언트는
 * 같은 요청에 answers 만 붙여 다시 보낸다 — 서버가 세션을 들고 있지 않는다.
 */
@Getter
@NoArgsConstructor
public class StartRequest {
    private String planRequestId;
    private EligibilityRequest eligibilityRequest;

    /** 우대금리 답변 (itemKey → 체크 1 / 수 n). null = 아직 질문 전 */
    private Map<String, Integer> answers;
}
