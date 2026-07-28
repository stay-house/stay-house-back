package com.example.stay_house_back.dto;

public record HouseSearchResponse(
        String buildingName,
        String roadAddress,
        String sigunguCd,   // 법정동코드 앞 5자리
        String bjdongCd,    // 법정동코드 뒤 5자리
        String bun,         // 번
        String ji           // 지
) {}
