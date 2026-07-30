package com.example.stay_house_back.service;

import com.example.stay_house_back.dto.preferential.PlanLoanValues;
import com.example.stay_house_back.dto.preferential.PreferentialEvaluation;
import com.example.stay_house_back.dto.preferential.PreferentialProfile;
import com.example.stay_house_back.dto.preferential.PreferentialQuestion;
import com.example.stay_house_back.entity.PolicyPreferentialCap;
import com.example.stay_house_back.entity.PolicyPreferentialItem;
import com.example.stay_house_back.entity.enums.PreferentialStacking;
import com.example.stay_house_back.repository.PolicyPreferentialCapRepository;
import com.example.stay_house_back.repository.PolicyPreferentialItemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 우대금리 체크박스 단계의 런타임. LLM 은 없다 — 구조화는 오프라인 1회
 * (preferential.py)로 끝났고, 여기서는 결정론적으로만 동작한다.
 *
 * <p>흐름: plan_candidates 완료 → {@link #buildQuestions} 로 질문 목록 생성
 * (NEED_MORE_INFO) → 사용자 답변 → {@link #evaluate} 로 (정책 × 플랜)별 판정
 * → 확정 금리로 plan_finalize.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreferentialRateService {

    private final PolicyPreferentialItemRepository itemRepository;
    private final PolicyPreferentialCapRepository capRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── 질문 생성 (NEED_MORE_INFO) ─────────────────────────────────────────

    /**
     * 후보 정책들의 우대 항목을 itemKey 로 병합해 질문 목록을 만든다.
     *
     * <ul>
     *   <li>ASK 없는 항목(신청액 30% 이하)은 질문이 아니다 — evaluate 에서 자동 적용.
     *   <li>플랜 무관 PROFILE 조건(나이·소득·면적·보증금)에서 이미 탈락한 항목은
     *       질문을 띄우지 않는다. 27세 사용자는 "만 25세 미만 단독세대주"를 보지 않는다.
     *   <li>플랜 의존 조건(대출금)은 여기서 판정하지 않는다 — 플랜마다 값이 달라
     *       질문 단계에서는 자를 수 없고, evaluate 가 플랜별로 거른다.
     * </ul>
     *
     * 반환은 (영향 제도 수, 최대 인하폭) 내림차순 — 상위 N 개만 노출할 때 그대로 자른다.
     */
    public List<PreferentialQuestion> buildQuestions(Collection<Long> policyIds,
                                                     PreferentialProfile profile) {
        Map<String, List<PolicyPreferentialItem>> byKey = new LinkedHashMap<>();
        for (PolicyPreferentialItem item : itemRepository.findByPolicyIdIn(policyIds)) {
            List<Condition> conds = parseConditions(item);
            if (conds.stream().noneMatch(Condition::isAsk)) continue;   // 자동 항목
            if (!planIndependentProfilePasses(conds, profile)) continue;
            byKey.computeIfAbsent(item.getItemKey(), k -> new ArrayList<>()).add(item);
        }

        return byKey.entrySet().stream()
                .map(e -> {
                    List<PolicyPreferentialItem> items = e.getValue();
                    Map<Long, String> quotes = new LinkedHashMap<>();
                    items.forEach(i -> quotes.put(i.getPolicy().getId(), i.getSourceQuote()));
                    boolean count = parseConditions(items.get(0)).stream().anyMatch(Condition::isCount);
                    return PreferentialQuestion.builder()
                            .itemKey(e.getKey())
                            .question(items.get(0).getQuestion())
                            .answerType(count ? "COUNT" : "CHECK")
                            .maxDelta(items.stream().mapToDouble(PolicyPreferentialItem::getDelta).max().orElse(0))
                            .affectsPolicyIds(items.stream().map(i -> i.getPolicy().getId()).toList())
                            .sourceQuotes(quotes)
                            .build();
                })
                .sorted(Comparator
                        .comparingInt((PreferentialQuestion q) -> q.getAffectsPolicyIds().size()).reversed()
                        .thenComparing(Comparator.comparingDouble(PreferentialQuestion::getMaxDelta).reversed()))
                .toList();
    }

    // ─── 판정 (ANSWER 이후, 정책 × 플랜별) ──────────────────────────────────

    /**
     * 계산 순서 — 전부 결정론적이다:
     * <ol>
     *   <li>조건 충족 항목 수집 (PROFILE 전부 통과 + ASK 는 체크됐을 때)
     *   <li>그룹별로 EXCLUSIVE 는 max(delta) 1개, STACKABLE 은 전부 합산
     *   <li>sum = 그룹 합계
     *   <li>cap = 상한 규칙을 priority 순으로 평가해 첫 매치 (체크 결과 의존)
     *   <li>applied = min(sum, cap)
     * </ol>
     * 최종 금리는 {@link PreferentialEvaluation#applyTo}: max(base - applied, floor).
     */
    public PreferentialEvaluation evaluate(long policyId,
                                           PreferentialProfile profile,
                                           PlanLoanValues plan,
                                           Map<String, Integer> answers) {
        List<PolicyPreferentialItem> items = itemRepository.findByPolicyIdIn(List.of(policyId));

        // 1. 조건 충족 항목. COUNT 항목은 인하폭이 delta × 수 (예: 추가출산 자녀 1명당 0.2%p)
        Map<String, List<PolicyPreferentialItem>> groups = new LinkedHashMap<>();
        Map<String, Double> effectiveDelta = new LinkedHashMap<>();
        for (PolicyPreferentialItem item : items) {
            if (!satisfies(item, profile, plan, answers)) continue;
            boolean count = parseConditions(item).stream().anyMatch(Condition::isCount);
            double delta = count
                    ? item.getDelta() * answers.getOrDefault(item.getItemKey(), 0)
                    : item.getDelta();
            effectiveDelta.put(item.getItemKey(), delta);
            groups.computeIfAbsent(item.getGroupId(), g -> new ArrayList<>()).add(item);
        }

        // 2~3. 그룹 규칙 적용
        List<PreferentialEvaluation.AppliedItem> applied = new ArrayList<>();
        double sum = 0;
        for (List<PolicyPreferentialItem> group : groups.values()) {
            boolean exclusive = group.get(0).getStacking() == PreferentialStacking.EXCLUSIVE;
            PolicyPreferentialItem best = exclusive
                    ? group.stream().max(Comparator.comparingDouble(
                            i -> effectiveDelta.get(i.getItemKey()))).orElseThrow()
                    : null;
            for (PolicyPreferentialItem item : group) {
                boolean counts = !exclusive || item == best;
                double delta = effectiveDelta.get(item.getItemKey());
                if (counts) sum += delta;
                applied.add(PreferentialEvaluation.AppliedItem.builder()
                        .itemKey(item.getItemKey())
                        .delta(delta)
                        .applied(counts)
                        .sourceQuote(item.getSourceQuote())
                        .build());
            }
        }

        // 4. 상한 — 충족된 항목 키 기준. 상수로 두면 틀린다 (0.5 / 0.7 / 1.0)
        Set<String> satisfiedKeys = applied.stream()
                .map(PreferentialEvaluation.AppliedItem::getItemKey)
                .collect(java.util.stream.Collectors.toSet());
        double cap = 0.5;
        double floor = 1.0;
        for (PolicyPreferentialCap rule : capRepository.findByPolicyIdOrderByPriorityAsc(policyId)) {
            floor = rule.getFloorRate();
            List<String> when = parseKeys(rule.getWhenAnyChecked());
            if (when == null || when.stream().anyMatch(satisfiedKeys::contains)) {
                cap = rule.getCap();
                break;
            }
        }

        // 5. 적용
        return PreferentialEvaluation.builder()
                .items(applied)
                .sum(round(sum))
                .cap(cap)
                .applied(round(Math.min(sum, cap)))
                .floorRate(floor)
                .build();
    }

    // ─── 조건 판정 ──────────────────────────────────────────────────────────

    private boolean satisfies(PolicyPreferentialItem item, PreferentialProfile profile,
                              PlanLoanValues plan, Map<String, Integer> answers) {
        for (Condition c : parseConditions(item)) {
            if (c.isAsk()) {
                if (answers.getOrDefault(item.getItemKey(), 0) < 1) return false;
            } else {
                Double actual = fieldValue(c.field, profile, plan);
                if (actual == null || !compare(actual, c.op, c.value)) return false;
            }
        }
        return true;
    }

    /** 플랜 의존 필드(LOAN_*)는 질문 단계에서 판정하지 않는다 — 통과로 취급. */
    private boolean planIndependentProfilePasses(List<Condition> conds, PreferentialProfile profile) {
        for (Condition c : conds) {
            if (c.isAsk() || c.field.startsWith("LOAN_")) continue;
            Double actual = fieldValue(c.field, profile, null);
            if (actual == null || !compare(actual, c.op, c.value)) return false;
        }
        return true;
    }

    private Double fieldValue(String field, PreferentialProfile profile, PlanLoanValues plan) {
        return switch (field) {
            case "AGE" -> profile.age() == null ? null : profile.age().doubleValue();
            case "ANNUAL_INCOME" -> profile.annualIncome() == null ? null : profile.annualIncome().doubleValue();
            case "AREA_SQM" -> profile.areaSqm();
            case "DEPOSIT" -> profile.deposit() == null ? null : profile.deposit().doubleValue();
            case "LOAN_AMOUNT" -> plan == null ? null : (double) plan.loanAmount();
            case "LOAN_RATIO_OF_CAP" -> (plan == null || plan.capAmount() <= 0)
                    ? null : (double) plan.loanAmount() / plan.capAmount();
            default -> null;   // 미지의 필드 = 불충족. 우대는 보수적으로
        };
    }

    private boolean compare(double actual, String op, double value) {
        return switch (op) {
            case "LE" -> actual <= value;
            case "LT" -> actual < value;
            case "GE" -> actual >= value;
            case "GT" -> actual > value;
            default -> false;
        };
    }

    // ─── JSON 파싱 ──────────────────────────────────────────────────────────

    record Condition(String type, String field, String op, Double value, String input) {
        boolean isAsk() { return "ASK".equals(type); }
        boolean isCount() { return isAsk() && "COUNT".equals(input); }
    }

    private List<Condition> parseConditions(PolicyPreferentialItem item) {
        try {
            return objectMapper.readValue(item.getConditions(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException(
                    "conditions JSON 파싱 실패 — item id=" + item.getId(), e);
        }
    }

    private List<String> parseKeys(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("when_any_checked JSON 파싱 실패: " + json, e);
        }
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;   // 0.1+0.2 부동소수점 잔재 제거
    }
}
