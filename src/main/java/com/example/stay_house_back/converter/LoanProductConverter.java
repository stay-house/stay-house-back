package com.example.stay_house_back.converter;

import com.example.stay_house_back.dto.eligibility.EligibleLoanProductDto;
import com.example.stay_house_back.entity.EligibilityCondition;
import com.example.stay_house_back.entity.LoanProduct;
import com.example.stay_house_back.entity.LoanProductRateOption;

import java.util.List;
import java.util.OptionalDouble;

public class LoanProductConverter {

    private LoanProductConverter() {}

    public static EligibleLoanProductDto toDto(EligibilityCondition ec, List<LoanProductRateOption> rateOptions) {
        LoanProduct p = ec.getLoanProduct();

        OptionalDouble rateMin = rateOptions.stream()
                .filter(r -> r.getLendRateMin() != null)
                .mapToDouble(LoanProductRateOption::getLendRateMin)
                .min();
        OptionalDouble rateMax = rateOptions.stream()
                .filter(r -> r.getLendRateMax() != null)
                .mapToDouble(LoanProductRateOption::getLendRateMax)
                .max();

        return EligibleLoanProductDto.builder()
                .id(p.getId())
                .bankName(p.getBankNm())
                .productName(p.getFinPrdtNm())
                .joinWay(p.getJoinWay())
                .productClass(p.getProductClass() != null ? p.getProductClass().name() : null)
                .isYouth(p.getIsYouth())
                .guaranteeAgency(p.getGuaranteeAgency())
                .maxAmount(p.getMaxAmount())
                .ltvRatio(p.getLtvRatio())
                .rateMin(rateMin.isPresent() ? rateMin.getAsDouble() : null)
                .rateMax(rateMax.isPresent() ? rateMax.getAsDouble() : null)
                .housingTarget(p.getHousingTarget() != null ? p.getHousingTarget().name() : null)
                .sourceUrl(ec.getSourceUrl())
                .build();
    }
}
