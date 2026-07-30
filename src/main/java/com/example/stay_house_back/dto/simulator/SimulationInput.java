package com.example.stay_house_back.dto.simulator;

import com.example.stay_house_back.dto.plan.Plan;
import com.example.stay_house_back.dto.eligibility.HousingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationInput {

    private Plan plan;
    private HousingType housingType;
    private long monthlyRent;       // 월세 (전세면 0)
    private long maintenanceFee;    // 관리비
    private long monthlyIncomeNet;  // 세후 월소득

    // 대출이 있는 경우 사용자가 선택한 값 (SELF_FUNDED면 무시)
    private long loanAmount;
    private double annualRate;      // 실적용 금리 (lend_rate_min 기준, %)
    private int termMonths;         // 대출 기간 (개월)
    private boolean variableRate;   // true = 변동금리 (스트레스 시나리오 적용)
}
