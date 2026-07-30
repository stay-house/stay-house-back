package com.example.stay_house_back.dto.plan;

import com.example.stay_house_back.dto.eligibility.EligiblePolicyDto;
import lombok.Getter;

/**
 * 하나의 주거 금융 플랜.
 * 플랜 = 대출 방법(택1) × 월세지원(독립축, null = 지원 없음)
 */
@Getter
public class Plan {

    private final String planId;
    private final FundingSource fundingSource;
    private final EligiblePolicyDto rentSubsidy; // null = 월세지원 없음

    private Plan(String planId, FundingSource fundingSource, EligiblePolicyDto rentSubsidy) {
        this.planId = planId;
        this.fundingSource = fundingSource;
        this.rentSubsidy = rentSubsidy;
    }

    public static Plan of(FundingSource fundingSource, EligiblePolicyDto rentSubsidy) {
        String planId = buildPlanId(fundingSource, rentSubsidy);
        return new Plan(planId, fundingSource, rentSubsidy);
    }

    private static String buildPlanId(FundingSource fs, EligiblePolicyDto subsidy) {
        String fundingPart = switch (fs.getType()) {
            case SELF_FUNDED -> "SELF_FUNDED";
            case BANK_LOAN -> "BANK_LOAN_" + fs.getBankLoan().getId();
            case POLICY_LOAN -> "POLICY_LOAN_" + fs.getPolicyLoan().getId();
        };
        String subsidyPart = subsidy != null ? "SUBSIDY_" + subsidy.getId() : "NO_SUBSIDY";
        return fundingPart + "_" + subsidyPart;
    }
}
