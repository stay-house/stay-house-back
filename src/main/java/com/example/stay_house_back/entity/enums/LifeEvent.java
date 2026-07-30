package com.example.stay_house_back.entity.enums;

/**
 * 그 제도의 존재 이유가 되는 생애사건.
 *
 * <p>나이·소득과 달리 모름을 통과로 처리하면 안 된다. 전세피해를 겪지 않은
 * 사용자에게 전세피해 대출이 추천된다.
 */
public enum LifeEvent {

    NONE,

    /** 대출접수일 기준 2년 내 출산 (’23.1.1. 이후 출생아부터). */
    NEWBORN,

    /** 혼인기간 7년 이내 또는 3개월 이내 결혼예정자. */
    NEWLYWED,

    /** 전세피해주택 보증금 5억원 이하이며 보증금의 30% 이상 피해. */
    JEONSE_VICTIM
}
