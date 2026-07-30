package com.example.stay_house_back.entity;

import com.example.stay_house_back.entity.enums.PreferentialStacking;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

/**
 * 우대금리 구조화 항목. 원문은 policy_preferential_rate.raw_text 에 있고,
 * 이 테이블은 LLM 이 1회 추출해 사람이 검수한 결과다 (preferential.py --load).
 */
@Entity
@Table(name = "policy_preferential_item")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PolicyPreferentialItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stacking", nullable = false, length = 10)
    private PreferentialStacking stacking;

    /** 제도 간 중복 제거 키. 같은 키는 같은 질문이라 한 번만 묻는다. */
    @Column(name = "item_key", nullable = false)
    private String itemKey;

    /** null = ASK 없음. 조건 충족 시 자동 적용 (예: 신청액 30% 이하). */
    @Column(name = "question")
    private String question;

    /** 인하폭(%p). */
    @Column(name = "delta", nullable = false)
    private Double delta;

    /** JSON 배열. PROFILE(코드 판정) / ASK(질문) 조건. */
    @Column(name = "conditions", nullable = false, columnDefinition = "TEXT")
    private String conditions;

    /** raw_text 의 부분 문자열. 적재 시 검증되므로 화면에 근거로 표시할 수 있다. */
    @Column(name = "source_quote", nullable = false, columnDefinition = "TEXT")
    private String sourceQuote;

    @Column(name = "checked_by")
    private String checkedBy;

    @Column(name = "checked_at")
    private String checkedAt;
}
