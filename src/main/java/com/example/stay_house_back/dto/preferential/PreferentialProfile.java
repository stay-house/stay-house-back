package com.example.stay_house_back.dto.preferential;

/**
 * PROFILE 조건 판정에 쓰는 값 중 플랜과 무관한 것들.
 * 사용자(나이·연소득)와 매물(면적·보증금)에서 나온다.
 * null 인 값과 비교하는 조건은 불충족으로 본다 — 우대는 보수적으로.
 */
public record PreferentialProfile(
        Integer age,
        Integer annualIncome,
        Double areaSqm,
        Long deposit
) {}
