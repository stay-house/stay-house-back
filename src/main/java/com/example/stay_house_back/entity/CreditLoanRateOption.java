package com.example.stay_house_back.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "credit_loan_rate_option")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreditLoanRateOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_loan_product_id", nullable = false)
    private CreditLoanProduct creditLoanProduct;

    @Column(name = "dcls_month", length = 10)
    private String dclsMonth;

    @Column(name = "fin_co_no", length = 20)
    private String finCoNo;

    @Column(name = "fin_prdt_cd", length = 50)
    private String finPrdtCd;

    @Column(name = "crdt_prdt_type", length = 10)
    private String crdtPrdtType;

    @Column(name = "crdt_lend_rate_type", length = 5)
    private String crdtLendRateType;

    @Column(name = "crdt_lend_rate_type_nm")
    private String crdtLendRateTypeNm;

    @Column(name = "grade_900_over")
    private Double grade900Over;

    @Column(name = "grade_801_900")
    private Double grade801900;

    @Column(name = "grade_701_800")
    private Double grade701800;

    @Column(name = "grade_601_700")
    private Double grade601700;

    @Column(name = "grade_501_600")
    private Double grade501600;

    @Column(name = "grade_401_500")
    private Double grade401500;

    @Column(name = "grade_301_400")
    private Double grade301400;

    @Column(name = "grade_300_under")
    private Double grade300Under;

    @Column(name = "grade_avg")
    private Double gradeAvg;
}
