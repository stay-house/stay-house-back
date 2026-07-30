package com.example.stay_house_back.dto.eligibility;

import com.example.stay_house_back.dto.eligibility.HousingType;
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
    // 순자산가액(원). 기금 제도는 대부분 3.45억(전세피해 5.11억) 상한이 붙는다.
    private Integer netAsset;
    private Integer employmentMonths;  // 재직기간(개월). 급여소득자일 때만 필수
    private boolean noHouse;           // true = 무주택
    private Integer houseCount;        // 무주택이 아닐 때 주택 보유 수
    private boolean hasExistingJeonseLoan; // 기존 전세자금대출 보유 여부
    private Integer creditScoreKcb;    // 선택
    private Integer creditScoreNice;   // 선택

    // 생애사건. 셋 다 Boolean(nullable) 이다 — false 를 기본값으로 두면
    // 묻지 않은 조건을 '해당 없음'으로 단정하게 된다.

    /** 대출접수일 기준 2년 내 출산. */
    private Boolean hasNewborn;

    /**
     * 혼인기간 7년 이내 또는 3개월 이내 결혼예정자.
     * married 로 대신할 수 없다 — 결혼예정자를 포함하고 혼인 10년차를 제외한다.
     */
    private Boolean newlywed;

    /** 전세피해주택 보증금 5억원 이하이며 보증금의 30% 이상 피해. */
    private Boolean jeonseVictim;

    // 매물 정보
    private String address;
    private String buildingName;
    private double areaSqm;            // 전용면적(m²)
    private HousingType housingType;   // JEONSE / MONTHLY_RENT
    private int deposit;               // 보증금(원)
    private Integer monthlyRent;       // 월세(원). 전세면 null
    private Integer maintenanceFee;    // 관리비(원/월)
}
