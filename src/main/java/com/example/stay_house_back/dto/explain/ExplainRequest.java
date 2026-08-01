package com.example.stay_house_back.dto.explain;

import com.example.stay_house_back.dto.ws.PlanDto;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상세 화면의 LLM 설명 요청. 무상태 — 클라이언트가 RESULT 로 받은 플랜을
 * 그대로 echo 한다. 서버는 아무것도 재계산하지 않는다 (D18: LLM 은 설명만).
 */
@Getter
@NoArgsConstructor
public class ExplainRequest {
    private PlanDto plan;
    private String buildingName;
}
