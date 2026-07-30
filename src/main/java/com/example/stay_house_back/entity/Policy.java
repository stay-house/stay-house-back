package com.example.stay_house_back.entity;

import com.example.stay_house_back.entity.enums.BudgetStatus;
import com.example.stay_house_back.entity.enums.ExtractionStatus;
import com.example.stay_house_back.entity.enums.PolicyCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "policy")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_nm", nullable = false)
    private String policyNm;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private PolicyCategory category;

    @Column(name = "source_code", unique = true)
    private String sourceCode;

    @Column(name = "operating_agency")
    private String operatingAgency;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "target_text", columnDefinition = "TEXT")
    private String targetText;

    @Column(name = "raw_path")
    private String rawPath;

    @Column(name = "rate_text")
    private String rateText;

    @Column(name = "limit_text")
    private String limitText;

    @Column(name = "term_text")
    private String termText;

    // POLICY_LOAN fields
    @Column(name = "loan_lmt_min")
    private Integer loanLmtMin;

    @Column(name = "loan_lmt_max")
    private Integer loanLmtMax;

    @Column(name = "rate_min")
    private Double rateMin;

    @Column(name = "rate_max")
    private Double rateMax;

    @Column(name = "guarantee_agency", length = 20)
    private String guaranteeAgency;

    // RENT_SUBSIDY fields
    @Column(name = "monthly_amount")
    private Integer monthlyAmount;

    @Column(name = "max_duration_months")
    private Integer maxDurationMonths;

    // GUARANTEE_FEE_REFUND fields
    @Column(name = "refund_rate")
    private Double refundRate;

    @Column(name = "refund_cap")
    private Integer refundCap;

    @Column(name = "apply_start_date", length = 20)
    private String applyStartDate;

    @Column(name = "apply_end_date", length = 20)
    private String applyEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_status", length = 20)
    private BudgetStatus budgetStatus = BudgetStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", length = 20)
    private ExtractionStatus extractionStatus = ExtractionStatus.RAW_ONLY;

    @Column(name = "dcls_month", length = 10)
    private String dclsMonth;

    @Column(name = "fetched_at")
    private String fetchedAt;

    @Column(name = "source_type", length = 20)
    private String sourceType = "MANUAL";

    @Column(name = "monthly_limit")
    private Integer monthlyLimit;

    @Column(name = "loan_lmt_metro")
    private Integer loanLmtMetro;

    @Column(name = "loan_lmt_other")
    private Integer loanLmtOther;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PolicyRateMatrix> rateMatrices = new ArrayList<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PolicyPreferentialRate> preferentialRates = new ArrayList<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PolicyRateRule> rateRules = new ArrayList<>();
}
