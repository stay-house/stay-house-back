package com.example.stay_house_back.controller;

import com.example.stay_house_back.dto.explain.ExplainRequest;
import com.example.stay_house_back.dto.explain.RateOptionView;
import com.example.stay_house_back.dto.simulator.SimulationInput;
import com.example.stay_house_back.dto.simulator.SimulationResult;
import com.example.stay_house_back.dto.ws.PlanDto;
import com.example.stay_house_back.repository.EligibilityConditionRepository;
import com.example.stay_house_back.repository.LoanProductRateOptionRepository;
import com.example.stay_house_back.service.ScenarioSimulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 상세 화면의 금리옵션 비교 (D20: 재계산은 서버가 한다).
 * 랭킹은 자동선택(변동 우선·만기일시 관행)으로 굳히고, 여기서만 사용자가 옵션을
 * 바꿔 본다 — 순위에는 영향을 주지 않는다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PlanRateOptionController {

    private final LoanProductRateOptionRepository rateOptionRepository;
    private final EligibilityConditionRepository eligibilityConditionRepository;
    private final ScenarioSimulatorService scenarioSimulatorService;

    @PostMapping("/api/plan/rate-options")
    @Transactional(readOnly = true)
    public List<RateOptionView> rateOptions(@RequestBody ExplainRequest request) {
        PlanDto plan = request.getPlan();
        if (plan.getProductId() == null || plan.getFundingType() == null
                || !"BANK_LOAN".equals(plan.getFundingType().name())) {
            return List.of();   // 옵션 선택이 존재하는 건 은행 상품뿐 (정책=격자 1개, 신용=점수 결정)
        }

        // 분할상환 계산의 n. 자격조건의 최초 대출기간 — 전세 18/18 채워져 있고, 없으면 관행 24개월
        int termMonths = eligibilityConditionRepository.findByLoanProductId(plan.getProductId())
                .map(ec -> ec.getLoanTermMonth() != null ? ec.getLoanTermMonth() : 24)
                .orElse(24);

        return rateOptionRepository.findByLoanProductId(plan.getProductId()).stream()
                .filter(o -> o.getLendRateMin() != null)
                .map(o -> {
                    boolean variable = o.getLendRateTypeNm() != null && o.getLendRateTypeNm().contains("변동");
                    boolean amortizing = o.getRpayTypeNm() != null && o.getRpayTypeNm().contains("분할");
                    SimulationResult sim = scenarioSimulatorService.simulate(SimulationInput.builder()
                            .planId(plan.getPlanId())
                            .hasLoan(true)
                            .govRentSubsidyAmount(plan.getGovRentSubsidyAmount() != null ? plan.getGovRentSubsidyAmount() : 0L)
                            .monthlyRent(plan.getMonthlyRent())
                            .maintenanceFee(plan.getMaintenanceFee())
                            .monthlyIncomeNet(plan.getMonthlyIncomeNet())
                            .loanAmount(plan.getLoanAmount())
                            .annualRate(o.getLendRateMin())
                            .variableRate(variable)
                            .amortizing(amortizing)
                            .termMonths(termMonths)
                            .build());
                    return RateOptionView.builder()
                            .rateOptionId(o.getId())
                            .repayTypeName(o.getRpayTypeNm())
                            .rateTypeName(o.getLendRateTypeNm())
                            .variableRate(variable)
                            .amortizing(amortizing)
                            .annualRate(o.getLendRateMin())
                            .rateMax(o.getLendRateMax())
                            .termMonths(termMonths)
                            .rankingBasis(!amortizing && variable == plan.isVariableRate()
                                    && o.getLendRateMin() == plan.getAnnualRate())
                            .scenarios(sim.getScenarios())
                            .build();
                })
                .toList();
    }
}
