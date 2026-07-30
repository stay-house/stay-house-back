package com.example.stay_house_back.dto.preferential;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 한 (정책 × 플랜)의 우대금리 판정 결과.
 *
 * <p>sum 과 cap 을 둘 다 실어 보낸다 — 0.6%p 를 체크해도 적용은 0.5%p 인 경우를
 * 화면이 설명할 수 있어야 한다.
 */
@Getter
@Builder
public class PreferentialEvaluation {

    @Getter
    @Builder
    public static class AppliedItem {
        private final String itemKey;
        private final double delta;
        /** EXCLUSIVE 그룹에서 밀렸으면 false — "가장 유리한 항목만 반영" 안내용. */
        private final boolean applied;
        private final String sourceQuote;
    }

    /** 조건을 충족한 항목 전부 (EXCLUSIVE 에서 밀린 것 포함). */
    private final List<AppliedItem> items;

    /** 그룹 규칙 적용 후 합산값 (상한 적용 전). */
    private final double sum;

    /** 체크 결과로 정해진 상한. */
    private final double cap;

    /** min(sum, cap) — 실제 인하폭. */
    private final double applied;

    /** 우대 적용 후 최종금리 하한(연 %). */
    private final double floorRate;

    /** finalRate = max(baseRate - applied, floorRate) */
    public double applyTo(double baseRate) {
        return Math.max(baseRate - applied, floorRate);
    }
}
