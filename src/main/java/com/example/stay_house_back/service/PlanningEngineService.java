package com.example.stay_house_back.service;

import com.example.stay_house_back.dto.eligibility.EligibilityRequest;
import com.example.stay_house_back.dto.eligibility.EligibilityResponse;
import com.example.stay_house_back.dto.eligibility.EligibleLoanProductDto;
import com.example.stay_house_back.dto.eligibility.EligiblePolicyDto;
import com.example.stay_house_back.dto.plan.FundingSource;
import com.example.stay_house_back.dto.plan.Plan;
import com.example.stay_house_back.entity.PolicyRateMatrix;
import com.example.stay_house_back.entity.ProductExclusion;
import com.example.stay_house_back.entity.enums.ProductType;
import com.example.stay_house_back.repository.PolicyRateMatrixRepository;
import com.example.stay_house_back.repository.ProductExclusionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 대출 한도·금리 계산만 담당한다. 시뮬레이션은 포함하지 않는다.
 * 시뮬레이션은 우대금리 확정 후 WebSocketController 에서 1회 실행된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanningEngineService {

    private final PolicyRateMatrixRepository policyRateMatrixRepository;
    private final ProductExclusionRepository productExclusionRepository;

    public List<Plan> generate(EligibilityResponse eligible, EligibilityRequest req) {
        int deposit = req.getDeposit();
        long ownCapital = req.getOwnCapital();

        // 정책 역할별 분류
        List<EligiblePolicyDto> policyLoans = eligible.getPolicies().stream()
                .filter(p -> "POLICY_LOAN".equals(p.getCategory()))
                .toList();
        List<EligiblePolicyDto> rentSubsidies = eligible.getPolicies().stream()
                .filter(p -> "RENT_SUBSIDY".equals(p.getCategory()))
                .toList();

        // 월세지원 옵션 (null = 지원 없음)
        List<EligiblePolicyDto> subsidyOptions = new ArrayList<>();
        subsidyOptions.add(null);
        subsidyOptions.addAll(rentSubsidies);

        List<Plan> plans = new ArrayList<>();

        // ── SELF_FUNDED ──────────────────────────────────────────────────────
        long selfShortfall = Math.max(0L, deposit - ownCapital);
        FundingSource selfFunded = FundingSource.selfFunded();
        for (EligiblePolicyDto subsidy : subsidyOptions) {
            plans.add(Plan.builder()
                    .planId(Plan.buildPlanId(selfFunded, subsidy))
                    .fundingSource(selfFunded)
                    .rentSubsidy(subsidy)
                    .loanAmount(0L)
                    .capAmount(0L)
                    .shortfall(selfShortfall)
                    .annualRate(0.0)
                    .variableRate(false)
                    .build());
        }

        // ── BANK_LOAN ────────────────────────────────────────────────────────
        for (EligibleLoanProductDto lp : eligible.getLoanProducts()) {
            LoanCalc calc = calcLoan(lp.getLtvRatio(), lp.getMaxAmount(), lp.getRateMin(), true, deposit, ownCapital);
            FundingSource fs = FundingSource.ofBankLoan(lp);
            for (EligiblePolicyDto subsidy : subsidyOptions) {
                plans.add(Plan.builder()
                        .planId(Plan.buildPlanId(fs, subsidy))
                        .fundingSource(fs)
                        .rentSubsidy(subsidy)
                        .loanAmount(calc.loanAmount)
                        .capAmount(calc.capAmount)
                        .shortfall(calc.shortfall)
                        .annualRate(calc.annualRate)
                        .variableRate(calc.variableRate)
                        .build());
            }
        }

        // ── POLICY_LOAN ──────────────────────────────────────────────────────
        int annualIncome = req.getAnnualIncome() != null ? req.getAnnualIncome() : 0;
        for (EligiblePolicyDto pl : policyLoans) {
            Double rate = lookupPolicyRate(pl.getId(), annualIncome, deposit);
            if (rate == null) {
                log.debug("정책 금리 격자 매칭 없음 — policyId={}, income={}, deposit={}", pl.getId(), annualIncome, deposit);
                continue;
            }
            LoanCalc calc = calcLoan(pl.getLtvRatio(), pl.getLoanLmtMax(), rate, false, deposit, ownCapital);
            FundingSource fs = FundingSource.ofPolicyLoan(pl);
            for (EligiblePolicyDto subsidy : subsidyOptions) {
                plans.add(Plan.builder()
                        .planId(Plan.buildPlanId(fs, subsidy))
                        .fundingSource(fs)
                        .rentSubsidy(subsidy)
                        .loanAmount(calc.loanAmount)
                        .capAmount(calc.capAmount)
                        .shortfall(calc.shortfall)
                        .annualRate(rate)
                        .variableRate(false)
                        .build());
            }
        }

        // ── 상호배타 제거 ────────────────────────────────────────────────────
        Set<String> exclusions = loadExclusionKeys();
        plans.removeIf(plan -> isExcluded(plan, exclusions));

        return plans;
    }

    // 세후 월소득 산출. monthlyIncomeNet 미입력 시 annualIncome 기반 근사
    public long resolveMonthlyIncomeNet(EligibilityRequest req) {
        if (req.getMonthlyIncomeNet() != null) return req.getMonthlyIncomeNet();
        if (req.getAnnualIncome() == null || req.getAnnualIncome() == 0) return 0L;
        double ratio = req.getAnnualIncome() < 30_000_000 ? 0.85
                     : req.getAnnualIncome() < 50_000_000 ? 0.80
                     : req.getAnnualIncome() < 70_000_000 ? 0.75
                     : 0.70;
        return (long)(req.getAnnualIncome() * ratio / 12);
    }

    // 대출 한도 계산.
    private LoanCalc calcLoan(Double ltvRatio, Integer maxAmount, Double rate,
                               boolean variableRate, int deposit, long ownCapital) {
        long required = Math.max(0L, deposit - ownCapital);

        long ltvCap = ltvRatio != null ? (long)(deposit * ltvRatio) : Long.MAX_VALUE;
        long amtCap = maxAmount != null ? maxAmount.longValue() : Long.MAX_VALUE;
        long cap = Math.min(ltvCap, amtCap);

        long loaned = Math.min(required, cap);
        long shortfall = Math.max(0L, required - loaned);
        double annualRate = rate != null ? rate : 0.0;

        return new LoanCalc(loaned, cap, shortfall, annualRate, variableRate);
    }

    /**
     * 정책 금리 격자에서 사용자 소득·보증금에 해당하는 금리 조회.
     * 매칭 행이 없으면 null 반환 → 해당 정책은 플랜에서 제외.
     */
    private Double lookupPolicyRate(Long policyId, int annualIncome, int deposit) {
        List<PolicyRateMatrix> matches = policyRateMatrixRepository.findMatchingRates(policyId, annualIncome, deposit);
        if (matches.isEmpty()) return null;
        return matches.get(0).getRate();
    }

    private Set<String> loadExclusionKeys() {
        return productExclusionRepository.findAll().stream()
                .map(this::toExclusionKey)
                .collect(Collectors.toSet());
    }

    private String toExclusionKey(ProductExclusion e) {
        return e.getId().getProductType().name() + ":" + e.getId().getProductId()
                + "->" + e.getId().getExcludedProductType().name() + ":" + e.getId().getExcludedProductId();
    }

    private boolean isExcluded(Plan plan, Set<String> exclusions) {
        ProductType fundingType = plan.getFundingSource().getProductType();
        Long fundingId = plan.getFundingSource().getProductId();
        EligiblePolicyDto subsidy = plan.getRentSubsidy();

        if (fundingId == null || subsidy == null) return false;

        String key1 = fundingType.name() + ":" + fundingId + "->POLICY:" + subsidy.getId();
        String key2 = "POLICY:" + subsidy.getId() + "->" + fundingType.name() + ":" + fundingId;

        return exclusions.contains(key1) || exclusions.contains(key2);
    }

    private record LoanCalc(long loanAmount, long capAmount, long shortfall, double annualRate, boolean variableRate) {}
}
