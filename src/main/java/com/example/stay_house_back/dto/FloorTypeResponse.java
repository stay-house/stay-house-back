package com.example.stay_house_back.dto;

public record FloorTypeResponse(
        double exclusiveAreaSqm,  // 전용면적 ㎡ (자격 필터 area_limit 비교용)
        double supplyAreaSqm,     // 공급면적 ㎡ (화면 표시용)
        double pyeong             // 공급면적 기준 평형
) {}
