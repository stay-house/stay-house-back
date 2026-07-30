package com.example.stay_house_back.dto.preferential;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 사용자에게 보여줄 우대금리 체크박스 하나. itemKey 로 제도 간 병합이 끝난 상태다 —
 * 전자계약이 5개 제도에 걸려 있어도 이 객체는 하나다.
 */
@Getter
@Builder
public class PreferentialQuestion {

    /** 답변(ANSWER)의 키. 프론트는 이 값을 그대로 되돌려 보낸다. */
    private final String itemKey;

    private final String question;

    /**
     * CHECK = 체크박스(답 0/1), COUNT = 수 입력(답 0~n, 예: "자녀가 몇 명인가요?").
     * COUNT 항목의 인하폭은 delta × 수로 계산된다.
     */
    private final String answerType;

    /** 걸려 있는 제도들 중 최대 인하폭. 질문 정렬·강조에 쓴다. */
    private final double maxDelta;

    /** 이 항목이 영향을 주는 policy_id 목록. */
    private final List<Long> affectsPolicyIds;

    /** policy_id → 근거 원문 인용. 상세 화면 표시용. */
    private final Map<Long, String> sourceQuotes;
}
