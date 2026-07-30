package com.example.stay_house_back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "eligibility_condition")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EligibilityCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Exactly one of the following three FKs must be non-null (validated in @PrePersist)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id")
    private LoanProduct loanProduct;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_loan_product_id")
    private CreditLoanProduct creditLoanProduct;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "income_min")
    private Integer incomeMin;

    @Column(name = "income_max")
    private Integer incomeMax;

    @Column(name = "asset_limit")
    private Integer assetLimit;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "employment_months")
    private Integer employmentMonths;

    @Column(name = "house_owner_type", length = 20)
    private String houseOwnerType;

    @Column(name = "deposit_limit_metro")
    private Integer depositLimitMetro;

    @Column(name = "deposit_limit_other")
    private Integer depositLimitOther;

    @Column(name = "monthly_rent_limit")
    private Integer monthlyRentLimit;

    @Column(name = "area_limit")
    private Double areaLimit;

    @Column(name = "ltv_ratio")
    private Double ltvRatio;

    @Column(name = "loan_term_month")
    private Integer loanTermMonth;

    @Column(name = "extension_max")
    private Integer extensionMax;

    @Column(name = "other_loan_allowed")
    private Boolean otherLoanAllowed;

    @Column(name = "contract_paid_ratio")
    private Double contractPaidRatio;

    @Column(name = "guarantee_agency", length = 30)
    private String guaranteeAgency;

    @Column(name = "guarantee_fee_rate")
    private Double guaranteeFeeRate;

    @Column(name = "guarantee_fee_rate_text", columnDefinition = "TEXT")
    private String guaranteeFeeRateText;

    @Column(name = "credit_score_kcb_min")
    private Integer creditScoreKcbMin;

    @Column(name = "credit_score_nice_min")
    private Integer creditScoreNiceMin;

    @Column(name = "max_amount")
    private Integer maxAmount;

    @Column(name = "raw_condition_text", columnDefinition = "TEXT")
    private String rawConditionText;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "checked_by")
    private String checkedBy;

    @Column(name = "checked_at")
    private String checkedAt;

    @Column(name = "no_deposit_limit")
    private Boolean noDepositLimit;

}
