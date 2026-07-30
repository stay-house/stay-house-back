package com.example.stay_house_back.dto.simulator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyFlowSnapshot {

    private double rateOffsetPercent;
    private long totalMonthlyHousingCost;   // 월 주거비 합계 (월세-지원금 + 대출상환 + 관리비)
    private long monthlyLoanRepayment;      // 대출 상환분 합계 (대출 없으면 0)
    private long monthlyRentAfterSubsidy;   // 순 월세 (지원금 차감 후, 전세면 0)
    private double burdenRatio;             // 월주거비 / 세후월소득 (0~1+)
}
