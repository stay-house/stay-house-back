package com.example.stay_house_back.dto.ws;

import com.example.stay_house_back.dto.plan.FundingSourceType;
import com.example.stay_house_back.dto.simulator.MonthlyFlowSnapshot;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@JsonDeserialize(builder = PlanDto.PlanDtoBuilder.class)
@JsonPOJOBuilder(withPrefix = "")
public class PlanDto {

    private final String planId;
    private final FundingSourceType fundingType;

    // 대출 상품 식별 (SELF_FUNDED이면 null)
    private final Long productId;
    private final String productName;
    private final String bankName;          // BANK_LOAN일 때 은행명, 그 외 null

    // POLICY_LOAN이면 정책 카테고리
    private final String category;

    // 월세지원 (null = 없음)
    private final String rentSubsidyName;
    private final Long govRentSubsidyAmount;
    // 모집형 지원의 접수 상태 — EXHAUSTED 면 랭킹 카드에 마감 배지를 띄운다
    private final String rentSubsidyBudgetStatus;
    private final String rentSubsidyApplyEndDate;

    // 대출 계산 결과
    private final long loanAmount;          // 실제 대출금
    private final long capAmount;
    private final long shortfall;
    private final long ownFundingAmount;    // 내 자금 충당액 = 보증금 - 대출금
    private final double annualRate;
    private final boolean variableRate;

    // 재시뮬레이션 입력값 (후보 → echo → ANSWER 단계에서 사용)
    private final long monthlyRent;
    private final long maintenanceFee;
    private final long monthlyIncomeNet;
    private final boolean monthlyIncomeEstimated;

    // 시뮬레이션 결과 (RESULT 단계에서 채워짐, 후보 단계에서는 null)
    private final Double burdenRatio;
    private final Double burdenIncrease2pp;
    private final List<MonthlyFlowSnapshot> scenarios;

    // 랭킹 결과 (RESULT 단계에서 채워짐)
    private final Double score;                 // 0~100 최종 점수
    private final String oneLiner;              // 플랜 한줄 요약
    private final Long monthlyHousingCost;      // 월 예상 부담액 합계 (원, 기준 시나리오)
    private final Long monthlyLoanRepayment;    // 대출 상환분 (이자 또는 원리금, 기준 시나리오)
    private final Long monthlyRentAfterSubsidy; // 순 월세 (지원금 차감 후, 기준 시나리오)
}
