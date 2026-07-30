package com.example.stay_house_back.dto.preferential;

/**
 * PROFILE 조건 판정에 쓰는 값 중 플랜마다 달라지는 것들.
 * 한도 계산이 끝나야 나오므로 plan_candidates 이후에만 존재한다 —
 * Planning Engine 을 1차/2차로 쪼갠 이유가 이 값이다.
 */
public record PlanLoanValues(
        long loanAmount,   // 실제 대출금
        long capAmount     // 대출심사 산정금액 = min(ltvCap, maxAmount)
) {}
