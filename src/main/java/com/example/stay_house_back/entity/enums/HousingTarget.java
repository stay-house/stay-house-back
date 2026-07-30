package com.example.stay_house_back.entity.enums;

public enum HousingTarget {
    JEONSE,        // 전세 전용
    MONTHLY_RENT,  // 월세 전용
    BOTH,          // 전세·월세 모두 지원
    UNKNOWN        // 미분류 (LLM 재확인 필요, 보수적으로 전세 전용 취급)
}
