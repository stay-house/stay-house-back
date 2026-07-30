package com.example.stay_house_back.dto.eligibility;

import com.example.stay_house_back.dto.simulator.enums.HousingType;
import lombok.Getter;

@Getter
public class EligibilityRequest {

    // 사용자 정보
    private boolean korean;            // 내국인 여부
    private int age;
    private boolean married;           // 혼인 여부
    private EmploymentType employmentType;
    private Integer annualIncome;      // 연소득(원). 무소득자는 null 가능
    private Integer employmentMonths;  // 재직기간(개월). 급여소득자일 때만 필수
    private boolean noHouse;           // true = 무주택
    private Integer houseCount;        // 무주택이 아닐 때 주택 보유 수
    private boolean hasExistingJeonseLoan; // 기존 전세자금대출 보유 여부
    private Integer creditScoreKcb;    // 선택
    private Integer creditScoreNice;   // 선택

    // 매물 정보
    private String address;
    private String buildingName;
    private double areaSqm;            // 전용면적(m²)
    private HousingType housingType;   // JEONSE / MONTHLY_RENT
    private int deposit;               // 보증금(원)
    private Integer monthlyRent;       // 월세(원). 전세면 null
    private Integer maintenanceFee;    // 관리비(원/월)
}
