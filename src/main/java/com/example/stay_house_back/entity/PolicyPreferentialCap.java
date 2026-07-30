package com.example.stay_house_back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

/**
 * 우대 적용 상한. 체크 결과에 따라 상한이 달라지므로 상수로 둘 수 없다 —
 * 기본 0.5%p, 수급권자·차상위·한부모 1.0%p, 다자녀 0.7%p.
 * priority 오름차순으로 첫 매치를 적용한다.
 */
@Entity
@Table(name = "policy_preferential_cap")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PolicyPreferentialCap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "cap", nullable = false)
    private Double cap;

    /** JSON 배열(item_key 목록). null = 기본 규칙(항상 매치). */
    @Column(name = "when_any_checked", columnDefinition = "TEXT")
    private String whenAnyChecked;

    /** 우대 적용 후 최종금리 하한(연 %). */
    @Column(name = "floor_rate", nullable = false)
    private Double floorRate;
}
