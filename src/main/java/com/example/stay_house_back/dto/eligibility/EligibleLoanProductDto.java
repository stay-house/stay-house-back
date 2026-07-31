package com.example.stay_house_back.dto.eligibility;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EligibleLoanProductDto {
    private Long id;
    private String bankName;
    private String productName;
    private String joinWay;
    private String productClass;   // FUND / BANK
    private Boolean isYouth;
    private String guaranteeAgency;
    private Integer maxAmount;
    private Double ltvRatio;
    private Double rateMin;        // 자동선택 옵션(만기일시+변동 우선)의 lend_rate_min
    private Double rateMax;        // 금리 옵션 중 최고
    private Boolean isVariableRate; // 자동선택된 옵션의 변동금리 여부
    private String housingTarget;  // JEONSE / MONTHLY_RENT / BOTH / UNKNOWN
    private String sourceUrl;      // 자격조건 원본 URL
}
