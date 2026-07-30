package com.example.stay_house_back.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loan_product_rate_option")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LoanProductRateOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    @Column(name = "dcls_month", length = 10)
    private String dclsMonth;

    @Column(name = "fin_co_no", length = 20)
    private String finCoNo;

    @Column(name = "fin_prdt_cd", length = 50)
    private String finPrdtCd;

    @Column(name = "rpay_type", length = 10)
    private String rpayType;

    @Column(name = "rpay_type_nm")
    private String rpayTypeNm;

    @Column(name = "lend_rate_type", length = 10)
    private String lendRateType;

    @Column(name = "lend_rate_type_nm")
    private String lendRateTypeNm;

    @Column(name = "lend_rate_min")
    private Double lendRateMin;

    @Column(name = "lend_rate_max")
    private Double lendRateMax;

    @Column(name = "lend_rate_avg")
    private Double lendRateAvg;
}
