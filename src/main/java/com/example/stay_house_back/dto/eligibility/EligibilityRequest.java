package com.example.stay_house_back.dto.eligibility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRequest {

    // 사용자 정보
    private boolean korean;            // 내국인 여부
    private int age;
    private boolean married;           // 혼인 여부
    private EmploymentType employmentType;
    private Integer annualIncome;      // 연소득(원). 무소득자는 null 가능
    private Integer netAsset;          // 순자산가액(원)
    private Integer employmentMonths;  // 재직기간(개월). 급여소득자일 때만 필수
    private Boolean noHouse;           // true = 무주택, false = 보유, null = 모름
    private Integer houseCount;        // noHouse=false일 때 주택 보유 수
    private boolean hasExistingJeonseLoan; // 기존 전세자금대출 보유 여부

    // 대출접수일 기준 2년 내 출산
    private Boolean hasNewborn;

    // 혼인기간 7년 이내 또는 3개월 이내 결혼예정자.
    private Boolean newlywed;

    // 전세피해주택 보증금 5억원 이하이며 보증금의 30% 이상 피해.
    private Boolean jeonseVictim;

    // 매물 정보
    private String address;
    private String buildingName;
    private double areaSqm;            // 전용면적(m²)
    private HousingType housingType;   // JEONSE / MONTHLY_RENT
    private int deposit;               // 보증금(원)
    private Integer monthlyRent;       // 월세(원). 전세면 null
    private Integer maintenanceFee;    // 관리비(원/월)

    // 자금 정보
    private long ownCapital;           // 보증금으로 마련할 수 있는 자금(원)
    private Long monthlyIncomeNet;     // 세후 월소득(원). null이면 서비스에서 annualIncome 기반 근사
}
