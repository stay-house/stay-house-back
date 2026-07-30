package com.example.stay_house_back.entity;

import com.example.stay_house_back.entity.enums.ExtractionStatus;
import com.example.stay_house_back.entity.enums.HousingTarget;
import com.example.stay_house_back.entity.enums.ProductClass;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loan_product",
        uniqueConstraints = @UniqueConstraint(columnNames = {"dcls_month", "fin_co_no", "fin_prdt_cd"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dcls_month", nullable = false, length = 10)
    private String dclsMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fin_co_no", nullable = false)
    private FinancialInstitution financialInstitution;

    @Column(name = "fin_prdt_cd", nullable = false, length = 50)
    private String finPrdtCd;

    @Column(name = "kor_co_nm")
    private String korCoNm;

    @Column(name = "bank_nm")
    private String bankNm;

    @Column(name = "fin_prdt_nm")
    private String finPrdtNm;

    @Column(name = "join_way")
    private String joinWay;

    @Column(name = "loan_inci_expn", columnDefinition = "TEXT")
    private String loanInciExpn;

    @Column(name = "erly_rpay_fee", columnDefinition = "TEXT")
    private String erlyRpayFee;

    @Column(name = "dly_rate", columnDefinition = "TEXT")
    private String dlyRate;

    @Column(name = "loan_lmt", columnDefinition = "TEXT")
    private String loanLmt;

    @Column(name = "max_amount")
    private Integer maxAmount;

    @Column(name = "ltv_ratio")
    private Double ltvRatio;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_class", length = 10)
    private ProductClass productClass;

    @Column(name = "is_youth")
    private Boolean isYouth;

    @Column(name = "guarantee_agency", length = 20)
    private String guaranteeAgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", length = 20)
    private ExtractionStatus extractionStatus = ExtractionStatus.RAW_ONLY;

    @Column(name = "dcls_strt_day", length = 10)
    private String dclsStrtDay;

    @Column(name = "dcls_end_day", length = 10)
    private String dclsEndDay;

    @Column(name = "ingested_at")
    private String ingestedAt;

    @Column(name = "region_limit")
    private String regionLimit;

    @Column(name = "region_source")
    private String regionSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "housing_target", length = 20)
    private HousingTarget housingTarget;
}
