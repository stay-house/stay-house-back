package com.example.stay_house_back.converter;

import com.example.stay_house_back.dto.eligibility.EligibleLoanProductDto;
import com.example.stay_house_back.entity.EligibilityCondition;
import com.example.stay_house_back.entity.LoanProduct;
import com.example.stay_house_back.entity.LoanProductRateOption;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

public class LoanProductConverter {

    private LoanProductConverter() {}

    public static EligibleLoanProductDto toDto(EligibilityCondition ec, List<LoanProductRateOption> rateOptions) {
        LoanProduct p = ec.getLoanProduct();

        // 자동선택: 만기일시+변동 → 만기일시+고정 → 전체 최저 순으로 fallback
        List<LoanProductRateOption> valid = rateOptions.stream()
                .filter(r -> r.getLendRateMin() != null)
                .toList();

        LoanProductRateOption selected = valid.stream()
                .filter(r -> isBalloon(r) && isVariable(r))
                .min(Comparator.comparingDouble(LoanProductRateOption::getLendRateMin))
                .or(() -> valid.stream()
                        .filter(LoanProductConverter::isBalloon)
                        .min(Comparator.comparingDouble(LoanProductRateOption::getLendRateMin)))
                .or(() -> valid.stream()
                        .min(Comparator.comparingDouble(LoanProductRateOption::getLendRateMin)))
                .orElse(null);

        OptionalDouble rateMax = valid.stream()
                .mapToDouble(LoanProductRateOption::getLendRateMin)
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
                .ltvRatio(ec.getLtvRatio())
                .rateMin(selected != null ? selected.getLendRateMin() : null)
                .rateMax(rateMax.isPresent() ? rateMax.getAsDouble() : null)
                .isVariableRate(selected != null && isVariable(selected))
                .housingTarget(p.getHousingTarget() != null ? p.getHousingTarget().name() : null)
                .sourceUrl(ec.getSourceUrl())
                .build();
    }

    private static boolean isBalloon(LoanProductRateOption r) {
        return r.getRpayTypeNm() != null && r.getRpayTypeNm().contains("만기일시");
    }

    private static boolean isVariable(LoanProductRateOption r) {
        return r.getLendRateTypeNm() != null && r.getLendRateTypeNm().contains("변동");
    }
}
