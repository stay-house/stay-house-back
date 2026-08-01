package com.example.stay_house_back.converter;

import com.example.stay_house_back.dto.eligibility.EligiblePolicyDto;
import com.example.stay_house_back.entity.EligibilityCondition;
import com.example.stay_house_back.entity.Policy;

public class PolicyConverter {

    private PolicyConverter() {}

    public static EligiblePolicyDto toDto(EligibilityCondition ec) {
        Policy p = ec.getPolicy();
        return EligiblePolicyDto.builder()
                .id(p.getId())
                .policyName(p.getPolicyNm())
                .category(p.getCategory().name())
                .operatingAgency(p.getOperatingAgency())
                .rateMin(p.getRateMin())
                .rateMax(p.getRateMax())
                .loanLmtMax(p.getLoanLmtMax())
                .ltvRatio(ec.getLtvRatio())
                .monthlyAmount(p.getMonthlyAmount())
                .budgetStatus(p.getBudgetStatus() != null ? p.getBudgetStatus().name() : null)
                .applyEndDate(p.getApplyEndDate())
                .sourceUrl(p.getSourceUrl())
                .build();
    }
}
