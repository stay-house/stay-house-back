package com.example.stay_house_back.dto.simulator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResult {

    private String planTypeCode;
    private boolean hasVariableRate;        // 변동금리 대출 포함 여부
    private List<MonthlyFlowSnapshot> scenarios; // [기준, +1%p, +2%p]

    // 스코어링용 지표
    private double burdenRatio;             // 기준 시나리오 주거비부담률
    private double worstBurdenRatio;        // +2%p 시나리오 주거비부담률
    private double burdenIncrease2pp;       // worstBurdenRatio - burdenRatio

    public MonthlyFlowSnapshot baseScenario() {
        return scenarios.get(0);
    }

    public MonthlyFlowSnapshot worstScenario() {
        return scenarios.get(scenarios.size() - 1);
    }
}
