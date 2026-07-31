package com.example.stay_house_back.dto.plan;

import com.example.stay_house_back.dto.eligibility.EligiblePolicyDto;
import lombok.Builder;
import lombok.Getter;

/**
 * 하나의 주거 금융 플랜 — 대출 계산 결과만 담는다.
 * 시뮬레이션은 우대금리 확정 후 1회만 실행하므로 여기에 포함하지 않는다.
 */
@Getter
@Builder
public class Plan {

    private final String planId;
    private final FundingSource fundingSource;
    private final EligiblePolicyDto rentSubsidy;    // null = 월세지원 없음

    // 대출 계산 결과
    private final long loanAmount;      // 실제 대출 금액(원)
    private final long capAmount;       // min(ltvCap, maxAmount)
    private final long shortfall;       // 자기자금 추가 부담액(원). 0이면 대출로 전액 조달
    private final double annualRate;    // 기준 금리(%) — 우대금리 미적용
    private final boolean variableRate; // 변동금리 여부

    public static String buildPlanId(FundingSource fs, EligiblePolicyDto subsidy) {
        String fundingPart = switch (fs.getType()) {
            case SELF_FUNDED -> "SELF_FUNDED";
            case BANK_LOAN -> "BANK_LOAN_" + fs.getBankLoan().getId();
            case POLICY_LOAN -> "POLICY_LOAN_" + fs.getPolicyLoan().getId();
        };
        String subsidyPart = subsidy != null ? "SUBSIDY_" + subsidy.getId() : "NO_SUBSIDY";
        return fundingPart + "_" + subsidyPart;
    }
}
