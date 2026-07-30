package com.example.stay_house_back.service;

import com.example.stay_house_back.dto.plan.FundingSourceType;
import com.example.stay_house_back.dto.simulator.MonthlyFlowSnapshot;
import com.example.stay_house_back.dto.simulator.SimulationInput;
import com.example.stay_house_back.dto.simulator.SimulationResult;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ScenarioSimulatorService {

    private static final double[] STRESS_OFFSETS = {0.0, 1.0, 2.0};

    /**
     * 플랜 하나에 대해 금리 +0/+1/+2%p 스트레스 시나리오 3개를 계산한다.
     * - 변동금리 대출: 각 시나리오마다 offset 적용
     * - 고정금리 대출 또는 대출 없음: 3개 시나리오 모두 동일값
     */
    public SimulationResult simulate(SimulationInput input) {
        boolean hasLoan = input.getPlan().getFundingSource().getType() != FundingSourceType.SELF_FUNDED;
        boolean hasVariableRate = hasLoan && input.isVariableRate();

        List<MonthlyFlowSnapshot> scenarios = Arrays.stream(STRESS_OFFSETS)
                .mapToObj(offset -> calcScenario(input, hasLoan, offset))
                .toList();

        double burdenRatio = scenarios.get(0).getBurdenRatio();
        double worstBurdenRatio = scenarios.get(2).getBurdenRatio();

        return SimulationResult.builder()
                .planTypeCode(input.getPlan().getPlanId())
                .hasVariableRate(hasVariableRate)
                .scenarios(scenarios)
                .burdenRatio(burdenRatio)
                .worstBurdenRatio(worstBurdenRatio)
                .burdenIncrease2pp(worstBurdenRatio - burdenRatio)
                .build();
    }

    private MonthlyFlowSnapshot calcScenario(SimulationInput input, boolean hasLoan, double rateOffset) {
        long monthlyLoanRepayment = hasLoan
                ? calcMonthlyInterest(input, rateOffset)
                : 0L;

        long govRentSubsidy = input.getPlan().getRentSubsidy() != null
                ? input.getPlan().getRentSubsidy().getMonthlyAmount()
                : 0;

        long monthlyRentAfterSubsidy = Math.max(0L, input.getMonthlyRent() - govRentSubsidy);

        // 월 주거비 = 순 월세 + 대출 월 납부액 + 관리비
        long totalMonthlyHousingCost = monthlyRentAfterSubsidy + monthlyLoanRepayment + input.getMaintenanceFee();

        double burdenRatio = input.getMonthlyIncomeNet() > 0
                ? (double) totalMonthlyHousingCost / input.getMonthlyIncomeNet()
                : Double.MAX_VALUE;

        return MonthlyFlowSnapshot.builder()
                .rateOffsetPercent(rateOffset)
                .totalMonthlyHousingCost(totalMonthlyHousingCost)
                .monthlyLoanRepayment(monthlyLoanRepayment)
                .monthlyRentAfterSubsidy(monthlyRentAfterSubsidy)
                .burdenRatio(burdenRatio)
                .build();
    }

    // ─── 월 납부액 계산 (이자만 상환 고정) ───────────────────────────────────────

    /**
     * 전세·정책 대출 모두 만기일시상환(이자만 상환) 방식.
     * 변동금리이면 rateOffset 적용, 고정금리이면 기본 금리 사용.
     * 월이자 = 원금 × (연이율 / 100) / 12
     */
    private long calcMonthlyInterest(SimulationInput input, double rateOffset) {
        double effectiveRate = input.isVariableRate()
                ? input.getAnnualRate() + rateOffset
                : input.getAnnualRate();
        if (effectiveRate == 0) return 0L;
        return Math.round(input.getLoanAmount() * (effectiveRate / 100.0) / 12.0);
    }
}
