package com.example.stay_house_back.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "financial_institution")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FinancialInstitution {

    @Id
    @Column(name = "fin_co_no", length = 20)
    private String finCoNo;

    @Column(name = "kor_co_nm")
    private String korCoNm;

    @Column(name = "homp_url")
    private String hompUrl;

    @Column(name = "cal_tel")
    private String calTel;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;
}
