package com.example.stay_house_back.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "policy_rate_matrix")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PolicyRateMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "income_min")
    private Integer incomeMin;

    @Column(name = "income_max")
    private Integer incomeMax;

    @Column(name = "deposit_min")
    private Integer depositMin;

    @Column(name = "deposit_max")
    private Integer depositMax;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @Column(name = "income_type", length = 20)
    private String incomeType;

    @Column(name = "income_label")
    private String incomeLabel;

    @Column(name = "deposit_label")
    private String depositLabel;

    @Column(name = "source", length = 20)
    private String source = "auto";

    @Column(name = "checked_by")
    private String checkedBy;

    @Column(name = "checked_at")
    private String checkedAt;
}
