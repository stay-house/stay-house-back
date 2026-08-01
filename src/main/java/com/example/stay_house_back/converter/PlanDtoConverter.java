package com.example.stay_house_back.converter;

import com.example.stay_house_back.dto.plan.Plan;
import com.example.stay_house_back.dto.simulator.SimulationResult;
import com.example.stay_house_back.dto.ws.PlanDto;

public class PlanDtoConverter {

    private PlanDtoConverter() {}

    /** 후보 단계 변환. 시뮬레이션 필드는 null. */
    public static PlanDto toCandidate(Plan plan, long deposit, long monthlyRent, long maintenanceFee,
                                      long monthlyIncomeNet, boolean monthlyIncomeEstimated) {
        String productName = switch (plan.getFundingSource().getType()) {
            case BANK_LOAN -> plan.getFundingSource().getBankLoan().getProductName();
            case POLICY_LOAN -> plan.getFundingSource().getPolicyLoan().getPolicyName();
            case SELF_FUNDED -> null;
        };
        String bankName = plan.getFundingSource().getType() == com.example.stay_house_back.dto.plan.FundingSourceType.BANK_LOAN
                ? plan.getFundingSource().getBankLoan().getBankName() : null;
        String category = switch (plan.getFundingSource().getType()) {
            case POLICY_LOAN -> plan.getFundingSource().getPolicyLoan().getCategory();
            default -> null;
        };

        return PlanDto.builder()
                .planId(plan.getPlanId())
                .fundingType(plan.getFundingSource().getType())
                .productId(plan.getFundingSource().getProductId())
                .productName(productName)
                .bankName(bankName)
                .category(category)
                .rentSubsidyName(plan.getRentSubsidy() != null ? plan.getRentSubsidy().getPolicyName() : null)
                .govRentSubsidyAmount(plan.getRentSubsidy() != null && plan.getRentSubsidy().getMonthlyAmount() != null
                        ? plan.getRentSubsidy().getMonthlyAmount().longValue() : null)
                .rentSubsidyBudgetStatus(plan.getRentSubsidy() != null ? plan.getRentSubsidy().getBudgetStatus() : null)
                .rentSubsidyApplyEndDate(plan.getRentSubsidy() != null ? plan.getRentSubsidy().getApplyEndDate() : null)
                .loanAmount(plan.getLoanAmount())
                .capAmount(plan.getCapAmount())
                .shortfall(plan.getShortfall())
                .ownFundingAmount(deposit - plan.getLoanAmount())
                .annualRate(plan.getAnnualRate())
                .variableRate(plan.isVariableRate())
                .monthlyRent(monthlyRent)
                .maintenanceFee(maintenanceFee)
                .monthlyIncomeNet(monthlyIncomeNet)
                .monthlyIncomeEstimated(monthlyIncomeEstimated)
                .build();
    }

    /** 시뮬레이션 결과를 후보 DTO에 합쳐 최종 결과 DTO를 만든다. */
    public static PlanDto withSimulation(PlanDto candidate, double finalRate, SimulationResult sim) {
        return candidate.toBuilder()
                .annualRate(finalRate)
                .burdenRatio(sim.getBurdenRatio())
                .burdenIncrease2pp(sim.getBurdenIncrease2pp())
                .scenarios(sim.getScenarios())
                .build();
    }
}
