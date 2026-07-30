package com.example.stay_house_back.service;

import com.example.stay_house_back.converter.LoanProductConverter;
import com.example.stay_house_back.converter.PolicyConverter;
import com.example.stay_house_back.dto.eligibility.*;
import com.example.stay_house_back.dto.simulator.enums.HousingType;
import com.example.stay_house_back.entity.EligibilityCondition;
import com.example.stay_house_back.entity.enums.HousingTarget;
import com.example.stay_house_back.entity.enums.LifeEvent;
import com.example.stay_house_back.repository.EligibilityConditionRepository;
import com.example.stay_house_back.repository.LoanProductRateOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EligibilityFilterService {

    private final EligibilityConditionRepository eligibilityConditionRepository;
    private final LoanProductRateOptionRepository loanProductRateOptionRepository;

    public EligibilityResponse filter(EligibilityRequest req) {
        if (!req.isKorean()) {
            return EligibilityResponse.empty();
        }

        boolean isMetro = isMetroRegion(req.getAddress());
        int houseCount = req.isNoHouse() ? 0 : (req.getHouseCount() != null ? req.getHouseCount() : 0);

        List<EligibleLoanProductDto> loanProducts = eligibilityConditionRepository
                .findByLoanProductIsNotNull()
                .stream()
                .filter(ec -> isEligible(ec, req, isMetro, houseCount))
                .filter(ec -> supportsHousingType(ec.getLoanProduct().getHousingTarget(), req.getHousingType()))
                .map(ec -> LoanProductConverter.toDto(ec, loanProductRateOptionRepository.findByLoanProduct(ec.getLoanProduct())))
                .toList();

        List<EligiblePolicyDto> policies = eligibilityConditionRepository
                .findByPolicyIsNotNull()
                .stream()
                .filter(ec -> isEligible(ec, req, isMetro, houseCount))
                .map(ec -> PolicyConverter.toDto(ec))
                .toList();

        return new EligibilityResponse(loanProducts, policies);
    }

    // 자격 판정
    private boolean isEligible(EligibilityCondition ec, EligibilityRequest req,
                                boolean isMetro, int houseCount) {
        int annualIncome = req.getAnnualIncome() != null ? req.getAnnualIncome() : 0;

        // 나이
        if (ec.getAgeMin() != null && req.getAge() < ec.getAgeMin()) return false;
        if (ec.getAgeMax() != null && req.getAge() > ec.getAgeMax()) return false;

        // 연소득
        if (ec.getIncomeMin() != null && annualIncome < ec.getIncomeMin()) return false;
        if (ec.getIncomeMax() != null && annualIncome > ec.getIncomeMax()) return false;

        // 순자산 — 소득과 별개 축. 미입력은 나이·소득처럼 통과시킨다
        if (ec.getAssetLimit() != null && req.getNetAsset() != null
                && req.getNetAsset() > ec.getAssetLimit()) return false;

        // 생애사건 — 미입력을 통과시키지 않는다
        if (!satisfiesLifeEvent(ec.getRequiredLifeEvent(), req)) return false;

        // 소득유형 (DB: "급여소득자,사업소득자" 형태로 저장)
        if (ec.getEmploymentType() != null) {
            String userType = req.getEmploymentType().getKoreanName();
            List<String> allowed = Arrays.stream(ec.getEmploymentType().split(","))
                    .map(String::trim)
                    .toList();
            if (!allowed.contains(userType)) return false;
        }

        // 재직기간
        if (ec.getEmploymentMonths() != null) {
            int userMonths = req.getEmploymentMonths() != null ? req.getEmploymentMonths() : 0;
            if (userMonths < ec.getEmploymentMonths()) return false;
        }

        // 무주택 조건
        if ("무주택".equals(ec.getHouseOwnerType()) && houseCount > 0) return false;
        if ("무주택또는1주택".equals(ec.getHouseOwnerType()) && houseCount > 1) return false;

        // 보증금 한도 (no_deposit_limit = true면 스킵)
        if (!Boolean.TRUE.equals(ec.getNoDepositLimit())) {
            if (isMetro && ec.getDepositLimitMetro() != null
                    && req.getDeposit() > ec.getDepositLimitMetro()) return false;
            if (!isMetro && ec.getDepositLimitOther() != null
                    && req.getDeposit() > ec.getDepositLimitOther()) return false;
        }

        // 전용면적
        if (ec.getAreaLimit() != null && req.getAreaSqm() > ec.getAreaLimit()) return false;

        // 기존 전세자금대출 보유 여부
        if (Boolean.FALSE.equals(ec.getOtherLoanAllowed()) && req.isHasExistingJeonseLoan()) return false;

        // 신용점수 — 사용자가 입력 안 했으면 체크 스킵
        if (ec.getCreditScoreKcbMin() != null && req.getCreditScoreKcb() != null
                && req.getCreditScoreKcb() < ec.getCreditScoreKcbMin()) return false;
        if (ec.getCreditScoreNiceMin() != null && req.getCreditScoreNice() != null
                && req.getCreditScoreNice() < ec.getCreditScoreNiceMin()) return false;

        // 월세 상한 (RENT_SUBSIDY 정책용)
        if (ec.getMonthlyRentLimit() != null) {
            int rent = req.getMonthlyRent() != null ? req.getMonthlyRent() : 0;
            if (rent > ec.getMonthlyRentLimit()) return false;
        }

        return true;
    }

    /**
     * 생애사건 요건 충족 여부. 모름(null)은 통과시키지 않는다 —
     * 통과시키면 전세피해를 겪지 않은 사용자에게 전세피해 대출이 추천된다.
     *
     * <p>required 가 null 인 건 은행 전세 상품(이 축이 없다)이므로 통과다.
     * 정책 9건은 요건이 없으면 NONE 이 채워져 있다.
     */
    private boolean satisfiesLifeEvent(LifeEvent required, EligibilityRequest req) {
        if (required == null || required == LifeEvent.NONE) return true;
        return switch (required) {
            case NEWBORN -> Boolean.TRUE.equals(req.getHasNewborn());
            case NEWLYWED -> Boolean.TRUE.equals(req.getNewlywed());
            case JEONSE_VICTIM -> Boolean.TRUE.equals(req.getJeonseVictim());
            case NONE -> true;
        };
    }

    // 상품의 housing_target과 사용자 요청 housingType 매칭 여부.
    private boolean supportsHousingType(HousingTarget target, HousingType requested) {
        if (target == null) return requested == HousingType.JEONSE; // null이면 전세 전용으로 보수적 처리
        return switch (requested) {
            case JEONSE -> target == HousingTarget.JEONSE
                    || target == HousingTarget.BOTH
                    || target == HousingTarget.UNKNOWN;
            case MONTHLY_RENT -> target == HousingTarget.MONTHLY_RENT
                    || target == HousingTarget.BOTH;
        };
    }

    private boolean isMetroRegion(String address) {
        if (address == null) return false;
        return address.contains("서울") || address.contains("경기") || address.contains("인천");
    }
}
