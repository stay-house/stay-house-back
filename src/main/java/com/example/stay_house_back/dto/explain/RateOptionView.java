package com.example.stay_house_back.dto.explain;

import com.example.stay_house_back.dto.simulator.MonthlyFlowSnapshot;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 상세 화면의 금리옵션 1개 재계산 결과 (D6: 플랜 단위는 상품 × 금리옵션).
 * 그 상품에 실제로 존재하는 옵션만 내려간다 — 없는 조합을 만들어내지 않는다.
 */
@Getter
@Builder
public class RateOptionView {
    private final long rateOptionId;
    private final String repayTypeName;     // 만기일시상환방식 / 분할상환방식 (공시 원문)
    private final String rateTypeName;      // 고정금리 / 변동금리 (공시 원문)
    private final boolean variableRate;
    private final boolean amortizing;
    private final double annualRate;        // lend_rate_min — 랭킹과 같은 기준
    private final Double rateMax;           // min~max 밴드 표시용
    private final int termMonths;           // 분할상환 계산에 쓴 대출기간
    /** 랭킹 계산에 쓰인 기준 옵션인지 — 상세 첫 화면과 숫자가 일치해야 하는 옵션 */
    private final boolean rankingBasis;
    private final List<MonthlyFlowSnapshot> scenarios;
}
