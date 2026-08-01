package com.example.stay_house_back.dto.eligibility;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EligiblePolicyDto {
    private Long id;
    private String policyName;
    private String category;       // POLICY_LOAN / RENT_SUBSIDY / GUARANTEE_FEE_REFUND
    private String operatingAgency;
    private Double rateMin;
    private Double rateMax;
    private Integer loanLmtMax;
    private Double ltvRatio;       // eligibility_condition.ltv_ratio. null이면 한도 = loanLmtMax
    private Integer monthlyAmount; // RENT_SUBSIDY용
    private String budgetStatus;   // OPEN / CLOSING_SOON / EXHAUSTED — 마감 배지용
    private String applyEndDate;   // 신청 마감일. 상세 화면 안내용
    private String sourceUrl;
}
