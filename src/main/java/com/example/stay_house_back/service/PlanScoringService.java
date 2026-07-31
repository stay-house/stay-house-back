package com.example.stay_house_back.service;

import com.example.stay_house_back.converter.PlanDtoConverter;
import com.example.stay_house_back.dto.simulator.SimulationInput;
import com.example.stay_house_back.dto.simulator.SimulationResult;
import com.example.stay_house_back.dto.ws.PlanDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 시뮬레이션 → 정규화 점수 → 정렬 → top 5
 *
 * final_score = α × financial_score + β × shortfall_score + (1-α-β) × preference_score
 *              - SHORTFALL_PENALTY (부족분 > 0 이면 고정 감점)
 *
 * financial_score : burdenRatio 기반. 낮을수록 높은 점수 (0~100 정규화)
 * shortfall_score : 부족분 기반. 부족분이 클수록 낮은 점수 (0~100 정규화). 부족분 없으면 100점.
 * preference_score: 사용자 선호 boost 기반 (0~100 정규화)
 *
 * α: burdenRatio 편차 ≥ 5%p 이면 0.5 (재무 우선), 미만이면 0.35
 * β: 항상 0.3 (부족분은 실행 가능성이므로 고정 비중)
 * SHORTFALL_PENALTY: 부족분이 1원이라도 있으면 고정 30점 감점 → 부족분 없는 플랜이 항상 우선
 */
@Service
@RequiredArgsConstructor
public class PlanScoringService {

    private static final double SPREAD_THRESHOLD   = 0.05;
    private static final double ALPHA_HIGH         = 0.5;
    private static final double ALPHA_LOW          = 0.35;
    private static final double BETA               = 0.3;   // shortfall 고정 비중
    private static final double SHORTFALL_PENALTY  = 30.0;  // 부족분 있으면 고정 감점
    private static final int    TOP_N              = 5;

    private final ScenarioSimulatorService simulatorService;

    public List<PlanDto> rank(List<PlanDto> candidates, List<String> boostedPlanIds) {
        // planId별 boost 횟수 집계
        Map<String, Long> boostCounts = boostedPlanIds.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        // 시뮬레이션 실행
        List<SimulatedPlan> simulated = candidates.stream()
                .map(p -> new SimulatedPlan(p, simulate(p), boostCounts.getOrDefault(p.getPlanId(), 0L).intValue()))
                .toList();

        // burdenRatio 편차로 α 결정
        double minBurden = simulated.stream().mapToDouble(s -> s.sim.getBurdenRatio()).min().orElse(0);
        double maxBurden = simulated.stream().mapToDouble(s -> s.sim.getBurdenRatio()).max().orElse(0);
        double spread = maxBurden - minBurden;
        double alpha  = spread >= SPREAD_THRESHOLD ? ALPHA_HIGH : ALPHA_LOW;

        // shortfall 정규화 기준
        long maxShortfall = simulated.stream().mapToLong(s -> s.planDto.getShortfall()).max().orElse(0);

        // preference 정규화 기준
        int maxBoost = simulated.stream().mapToInt(s -> s.boostCount).max().orElse(0);

        return simulated.stream()
                .map(s -> {
                    double financialScore  = spread > 0
                            ? 100.0 * (maxBurden - s.sim.getBurdenRatio()) / spread
                            : 100.0;
                    double shortfallScore  = maxShortfall > 0
                            ? 100.0 * (maxShortfall - s.planDto.getShortfall()) / maxShortfall
                            : 100.0;
                    double preferenceScore = maxBoost > 0
                            ? 100.0 * s.boostCount / maxBoost
                            : 0.0;
                    double finalScore = alpha * financialScore + BETA * shortfallScore + (1 - alpha - BETA) * preferenceScore;
                    if (s.planDto.getShortfall() > 0) {
                        finalScore -= SHORTFALL_PENALTY;
                    }
                    return Map.entry(finalScore, s);
                })
                .sorted(Map.Entry.<Double, SimulatedPlan>comparingByKey(Comparator.reverseOrder()))
                .limit(TOP_N)
                .map(e -> PlanDtoConverter.withSimulation(
                                e.getValue().planDto,
                                e.getValue().planDto.getAnnualRate(),
                                e.getValue().sim)
                        .toBuilder()
                        .score(e.getKey())
                        .oneLiner(buildOneLiner(e.getValue().planDto, e.getValue().sim))
                        .monthlyHousingCost(e.getValue().sim.baseScenario().getTotalMonthlyHousingCost())
                        .monthlyLoanRepayment(e.getValue().sim.baseScenario().getMonthlyLoanRepayment())
                        .monthlyRentAfterSubsidy(e.getValue().sim.baseScenario().getMonthlyRentAfterSubsidy())
                        .build())
                .toList();
    }

    private SimulationResult simulate(PlanDto plan) {
        SimulationInput input = SimulationInput.builder()
                .planId(plan.getPlanId())
                .hasLoan(plan.getLoanAmount() > 0)
                .govRentSubsidyAmount(plan.getGovRentSubsidyAmount() != null ? plan.getGovRentSubsidyAmount() : 0L)
                .monthlyRent(plan.getMonthlyRent())
                .maintenanceFee(plan.getMaintenanceFee())
                .monthlyIncomeNet(plan.getMonthlyIncomeNet())
                .loanAmount(plan.getLoanAmount())
                .annualRate(plan.getAnnualRate())
                .variableRate(plan.isVariableRate())
                .build();
        return simulatorService.simulate(input);
    }

    private String buildOneLiner(PlanDto plan, SimulationResult sim) {
        // 조합 설명
        StringBuilder sb = new StringBuilder();
        if (plan.getProductName() != null) {
            sb.append(plan.getProductName());
        } else {
            sb.append("자기자본 전액 조달");
        }
        if (plan.getRentSubsidyName() != null) {
            sb.append(" + ").append(plan.getRentSubsidyName());
        }

        // 금리 특성
        if (plan.getLoanAmount() > 0) {
            sb.append(" · ").append(plan.isVariableRate() ? "변동금리" : "고정금리");
        }

        // 부담 평가
        double burden = sim.getBurdenRatio();
        if (plan.getShortfall() > 0) {
            sb.append(" · 자기자금 ").append(plan.getShortfall() / 10000).append("만원 추가 필요");
        } else if (burden >= 0.40) {
            sb.append(" · 자금 부담 높음");
        } else if (burden >= 0.30) {
            sb.append(" · 월 부담 주의");
        } else {
            sb.append(" · 월 부담 안정적");
        }

        return sb.toString();
    }

    private record SimulatedPlan(PlanDto planDto, SimulationResult sim, int boostCount) {}
}
