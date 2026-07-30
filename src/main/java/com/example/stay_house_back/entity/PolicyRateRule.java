package com.example.stay_house_back.entity;

import com.example.stay_house_back.entity.enums.PolicyRuleAppliesTo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "policy_rate_rule",
        uniqueConstraints = @UniqueConstraint(columnNames = {"policy_id", "applies_to", "tier_label"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PolicyRateRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", nullable = false, length = 10)
    private PolicyRuleAppliesTo appliesTo;

    @Column(name = "tier_label")
    private String tierLabel;

    @Column(name = "amount_min")
    private Integer amountMin;

    @Column(name = "amount_max")
    private Integer amountMax;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @Column(name = "condition_text", columnDefinition = "TEXT")
    private String conditionText;

    @Column(name = "source", length = 20)
    private String source = "auto";

    @Column(name = "checked_by")
    private String checkedBy;

    @Column(name = "checked_at")
    private String checkedAt;
}
