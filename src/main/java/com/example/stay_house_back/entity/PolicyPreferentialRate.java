package com.example.stay_house_back.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "policy_preferential_rate")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PolicyPreferentialRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;
}
