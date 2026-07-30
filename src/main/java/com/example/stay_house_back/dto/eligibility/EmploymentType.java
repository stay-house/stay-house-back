package com.example.stay_house_back.dto.eligibility;

public enum EmploymentType {
    EMPLOYED("급여소득자"),
    SELF_EMPLOYED("사업소득자"),
    OTHER("기타소득자"),
    NONE("무소득자");

    private final String koreanName;

    EmploymentType(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }
}
