package com.example.stay_house_back.entity;

import com.example.stay_house_back.entity.enums.ExtractionStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "credit_loan_product",
        uniqueConstraints = @UniqueConstraint(columnNames = {"dcls_month", "fin_co_no", "fin_prdt_cd", "crdt_prdt_type"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreditLoanProduct {

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

    @Column(name = "crdt_prdt_type", length = 10)
    private String crdtPrdtType;

    @Column(name = "crdt_prdt_type_nm")
    private String crdtPrdtTypeNm;

    @Column(name = "kor_co_nm")
    private String korCoNm;

    @Column(name = "fin_prdt_nm")
    private String finPrdtNm;

    @Column(name = "join_way")
    private String joinWay;

    @Column(name = "cb_name")
    private String cbName;

    @Column(name = "dcls_strt_day", length = 10)
    private String dclsStrtDay;

    @Column(name = "ingested_at")
    private String ingestedAt;

    @Column(name = "bank_nm")
    private String bankNm;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", length = 20)
    private ExtractionStatus extractionStatus = ExtractionStatus.RAW_ONLY;
}
