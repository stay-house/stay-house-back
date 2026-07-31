package com.example.stay_house_back.dto.simulator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationInput {

    private String planId;
    private boolean hasLoan;               // SELF_FUNDED이면 false
    private long govRentSubsidyAmount;     // 월세지원금(원). 없으면 0

    private long monthlyRent;              // 월세(원). 전세면 0
    private long maintenanceFee;           // 관리비(원/월)
    private long monthlyIncomeNet;         // 세후 월소득(원)

    // 대출이 있는 경우 (hasLoan = true)
    private long loanAmount;
    private double annualRate;             // 실적용 금리(%)
    private boolean variableRate;          // true = 변동금리
}
