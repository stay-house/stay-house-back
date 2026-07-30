package com.example.stay_house_back.service;

import com.example.stay_house_back.dto.preferential.PlanLoanValues;
import com.example.stay_house_back.dto.preferential.PreferentialEvaluation;
import com.example.stay_house_back.dto.preferential.PreferentialProfile;
import com.example.stay_house_back.dto.preferential.PreferentialQuestion;
import com.example.stay_house_back.entity.Policy;
import com.example.stay_house_back.entity.PolicyPreferentialCap;
import com.example.stay_house_back.entity.PolicyPreferentialItem;
import com.example.stay_house_back.entity.enums.PreferentialStacking;
import com.example.stay_house_back.repository.PolicyPreferentialCapRepository;
import com.example.stay_house_back.repository.PolicyPreferentialItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * 우대금리 런타임 — 질문 병합·PROFILE 필터·판정 계산.
 * 픽스처는 버팀목/청년전용의 실제 구조를 본떴다 (eligibility/preferential.yaml).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreferentialRateServiceTest {

    @Mock
    private PolicyPreferentialItemRepository itemRepository;

    @Mock
    private PolicyPreferentialCapRepository capRepository;

    @InjectMocks
    private PreferentialRateService service;

    private static final Policy BEOTIMMOK = Policy.builder().id(1L).policyNm("버팀목").build();
    private static final Policy YOUTH = Policy.builder().id(2L).policyNm("청년전용").build();

    // ─── 픽스처 ──────────────────────────────────────────────────────────────

    private static PolicyPreferentialItem item(Policy policy, String group,
                                               PreferentialStacking stacking, String key,
                                               String question, double delta, String conditions) {
        return PolicyPreferentialItem.builder()
                .policy(policy).groupId(group).stacking(stacking).itemKey(key)
                .question(question).delta(delta).conditions(conditions)
                .sourceQuote("원문: " + key)
                .build();
    }

    private static PolicyPreferentialCap cap(Policy policy, int priority, double cap, String when) {
        return PolicyPreferentialCap.builder()
                .policy(policy).priority(priority).cap(cap).whenAnyChecked(when).floorRate(1.0)
                .build();
    }

    private static final String ASK_ONLY = "[{\"type\":\"ASK\"}]";

    /** 버팀목 축소판: 배타(한부모 1.0 / 3자녀 0.7) + 합산(전자계약 0.1, 30% 자동 0.2) */
    private List<PolicyPreferentialItem> beotimmokItems() {
        return List.of(
                item(BEOTIMMOK, "BASE", PreferentialStacking.EXCLUSIVE, "SINGLE_PARENT",
                        "한부모가구이신가요?", 1.0,
                        "[{\"type\":\"PROFILE\",\"field\":\"ANNUAL_INCOME\",\"op\":\"LE\",\"value\":50000000},{\"type\":\"ASK\"}]"),
                item(BEOTIMMOK, "BASE", PreferentialStacking.EXCLUSIVE, "CHILD_3PLUS",
                        "자녀가 3명 이상인가요?", 0.7, ASK_ONLY),
                item(BEOTIMMOK, "ADDITIONAL", PreferentialStacking.STACKABLE, "E_CONTRACT",
                        "부동산 전자계약으로 체결할 예정인가요?", 0.1, ASK_ONLY),
                item(BEOTIMMOK, "ADDITIONAL", PreferentialStacking.STACKABLE, "LOAN_UNDER_30PCT",
                        null, 0.2,
                        "[{\"type\":\"PROFILE\",\"field\":\"LOAN_RATIO_OF_CAP\",\"op\":\"LE\",\"value\":0.3}]")
        );
    }

    private List<PolicyPreferentialCap> beotimmokCaps() {
        return List.of(
                cap(BEOTIMMOK, 1, 1.0, "[\"BASIC_LIVELIHOOD\",\"SINGLE_PARENT\"]"),
                cap(BEOTIMMOK, 2, 0.7, "[\"CHILD_3PLUS\"]"),
                cap(BEOTIMMOK, 9, 0.5, null)
        );
    }

    private static final PreferentialProfile PROFILE_27 =
            new PreferentialProfile(27, 40_000_000, 40.0, 150_000_000L);

    // ─── 질문 생성 ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buildQuestions")
    class BuildQuestions {

        @Test
        @DisplayName("같은 itemKey 는 제도가 달라도 질문 하나로 합쳐진다")
        void mergesByItemKey() {
            given(itemRepository.findByPolicyIdIn(any())).willReturn(List.of(
                    item(BEOTIMMOK, "ADDITIONAL", PreferentialStacking.STACKABLE, "E_CONTRACT",
                            "부동산 전자계약으로 체결할 예정인가요?", 0.1, ASK_ONLY),
                    item(YOUTH, "ADDITIONAL", PreferentialStacking.STACKABLE, "E_CONTRACT",
                            "부동산 전자계약으로 체결할 예정인가요?", 0.1, ASK_ONLY)));

            List<PreferentialQuestion> qs = service.buildQuestions(List.of(1L, 2L), PROFILE_27);

            assertThat(qs).hasSize(1);
            assertThat(qs.get(0).getAffectsPolicyIds()).containsExactly(1L, 2L);
            assertThat(qs.get(0).getSourceQuotes()).containsKeys(1L, 2L);
        }

        @Test
        @DisplayName("PROFILE 에서 탈락한 항목은 질문을 띄우지 않는다 — 27세에게 만25세 질문 없음")
        void dropsProfileFailedItems() {
            given(itemRepository.findByPolicyIdIn(any())).willReturn(List.of(
                    item(YOUTH, "ADDITIONAL", PreferentialStacking.STACKABLE, "YOUTH_SINGLE_HOUSEHOLD",
                            "단독세대주이신가요?", 0.3,
                            "[{\"type\":\"PROFILE\",\"field\":\"AGE\",\"op\":\"LT\",\"value\":25},{\"type\":\"ASK\"}]")));

            assertThat(service.buildQuestions(List.of(2L), PROFILE_27)).isEmpty();
        }

        @Test
        @DisplayName("플랜 의존 조건(대출금)은 질문 단계에서 자르지 않는다")
        void keepsPlanDependentItems() {
            given(itemRepository.findByPolicyIdIn(any())).willReturn(List.of(
                    item(YOUTH, "ADDITIONAL", PreferentialStacking.STACKABLE, "YOUTH_SINGLE_HOUSEHOLD",
                            "단독세대주이신가요?", 0.3,
                            "[{\"type\":\"PROFILE\",\"field\":\"AGE\",\"op\":\"LT\",\"value\":25},"
                            + "{\"type\":\"PROFILE\",\"field\":\"LOAN_AMOUNT\",\"op\":\"LE\",\"value\":120000000},"
                            + "{\"type\":\"ASK\"}]")));
            PreferentialProfile age24 = new PreferentialProfile(24, 30_000_000, 40.0, 150_000_000L);

            assertThat(service.buildQuestions(List.of(2L), age24)).hasSize(1);
        }

        @Test
        @DisplayName("ASK 없는 자동 항목은 질문 목록에 없다")
        void skipsAutoItems() {
            given(itemRepository.findByPolicyIdIn(any())).willReturn(beotimmokItems());

            List<PreferentialQuestion> qs = service.buildQuestions(List.of(1L), PROFILE_27);

            assertThat(qs).extracting(PreferentialQuestion::getItemKey)
                    .doesNotContain("LOAN_UNDER_30PCT");
        }
    }

    // ─── 판정 계산 ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("evaluate")
    class Evaluate {

        private PreferentialEvaluation run(Map<String, Integer> answers, PlanLoanValues plan) {
            given(itemRepository.findByPolicyIdIn(any())).willReturn(beotimmokItems());
            given(capRepository.findByPolicyIdOrderByPriorityAsc(anyLong())).willReturn(beotimmokCaps());
            return service.evaluate(1L, PROFILE_27, plan, answers);
        }

        private static final PlanLoanValues LOAN_HIGH = new PlanLoanValues(100_000_000, 120_000_000);
        private static final PlanLoanValues LOAN_LOW = new PlanLoanValues(30_000_000, 120_000_000);

        @Test
        @DisplayName("배타 그룹은 체크된 것 중 최댓값 하나만 — 한부모+3자녀 = 1.0 (2.7 아님)")
        void exclusiveTakesMax() {
            PreferentialEvaluation ev = run(Map.of("SINGLE_PARENT", 1, "CHILD_3PLUS", 1), LOAN_HIGH);

            assertThat(ev.getSum()).isEqualTo(1.0);
            assertThat(ev.getItems()).filteredOn(i -> i.getItemKey().equals("CHILD_3PLUS"))
                    .singleElement()
                    .satisfies(i -> assertThat(i.isApplied()).isFalse());   // 밀린 항목도 내역에 남는다
        }

        @Test
        @DisplayName("상한이 체크 결과를 따라간다 — 한부모 체크 시 cap 1.0")
        void capDependsOnChecks() {
            assertThat(run(Map.of("SINGLE_PARENT", 1), LOAN_HIGH).getCap()).isEqualTo(1.0);
            assertThat(run(Map.of("CHILD_3PLUS", 1), LOAN_HIGH).getCap()).isEqualTo(0.7);
            assertThat(run(Map.of("E_CONTRACT", 1), LOAN_HIGH).getCap()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("sum 이 cap 을 넘으면 applied 는 cap — 둘 다 응답에 남는다")
        void appliedIsCappedButSumSurvives() {
            // 3자녀(0.7) + 전자계약(0.1) + 30%자동(0.2) = 1.0, cap 0.7
            PreferentialEvaluation ev = run(Map.of("CHILD_3PLUS", 1, "E_CONTRACT", 1), LOAN_LOW);

            assertThat(ev.getSum()).isEqualTo(1.0);
            assertThat(ev.getCap()).isEqualTo(0.7);
            assertThat(ev.getApplied()).isEqualTo(0.7);
        }

        @Test
        @DisplayName("자동 항목은 체크 없이도 플랜 값으로 판정된다 — 30% 이하일 때만")
        void autoItemFollowsPlanValues() {
            assertThat(run(Map.of(), LOAN_LOW).getApplied()).isEqualTo(0.2);    // 25% → 적용
            assertThat(run(Map.of(), LOAN_HIGH).getApplied()).isEqualTo(0.0);   // 83% → 미적용
        }

        @Test
        @DisplayName("PROFILE 미충족이면 체크해도 적용 안 됨 — 소득 초과 한부모")
        void profileGateBeatsCheck() {
            given(itemRepository.findByPolicyIdIn(any())).willReturn(beotimmokItems());
            given(capRepository.findByPolicyIdOrderByPriorityAsc(anyLong())).willReturn(beotimmokCaps());
            PreferentialProfile richProfile = new PreferentialProfile(27, 60_000_000, 40.0, 150_000_000L);

            PreferentialEvaluation ev = service.evaluate(1L, richProfile, LOAN_HIGH, Map.of("SINGLE_PARENT", 1));

            assertThat(ev.getSum()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("최종 금리는 하한 1.0% 아래로 내려가지 않는다")
        void floorHolds() {
            PreferentialEvaluation ev = run(Map.of("SINGLE_PARENT", 1), LOAN_HIGH);   // applied 1.0

            assertThat(ev.applyTo(2.5)).isEqualTo(1.5);
            assertThat(ev.applyTo(1.3)).isEqualTo(1.0);   // 1.3-1.0=0.3 → floor 1.0
        }

        @Test
        @DisplayName("아무것도 체크 안 하면 applied 0 — 기본금리 그대로")
        void nothingChecked() {
            PreferentialEvaluation ev = run(Map.of(), LOAN_HIGH);

            assertThat(ev.getApplied()).isEqualTo(0.0);
            assertThat(ev.applyTo(2.5)).isEqualTo(2.5);
        }
    }

    // ─── COUNT 답변 (신생아 특례의 "1명당" 항목) ─────────────────────────────

    @Nested
    @DisplayName("COUNT 항목")
    class CountItems {

        private static final String ASK_COUNT = "[{\"type\":\"ASK\",\"input\":\"COUNT\"}]";
        private static final Policy NEWBORN = Policy.builder().id(3L).policyNm("신생아특례").build();

        private PreferentialEvaluation run(Map<String, Integer> answers) {
            given(itemRepository.findByPolicyIdIn(any())).willReturn(List.of(
                    item(NEWBORN, "ADDITIONAL", PreferentialStacking.STACKABLE, "ADDL_BIRTH",
                            "대출접수일 기준 2년 내 추가 출산한 자녀가 몇 명인가요?", 0.2, ASK_COUNT),
                    item(NEWBORN, "ADDITIONAL", PreferentialStacking.STACKABLE, "MINOR_CHILD",
                            "출생 후 2년이 지난 미성년 자녀가 몇 명인가요?", 0.1, ASK_COUNT)));
            given(capRepository.findByPolicyIdOrderByPriorityAsc(anyLong()))
                    .willReturn(List.of(cap(NEWBORN, 9, 0.5, null)));
            return service.evaluate(3L, PROFILE_27, new PlanLoanValues(100_000_000, 100_000_000), answers);
        }

        @Test
        @DisplayName("인하폭은 delta × 수 — 추가출산 2명이면 0.4%p")
        void multipliesByCount() {
            assertThat(run(Map.of("ADDL_BIRTH", 2)).getApplied()).isEqualTo(0.4);
        }

        @Test
        @DisplayName("3명이면 0.6%p 지만 상한 0.5%p 가 자른다 — sum 은 그대로 남는다")
        void capTrimsMultiChild() {
            PreferentialEvaluation ev = run(Map.of("ADDL_BIRTH", 3));

            assertThat(ev.getSum()).isEqualTo(0.6);
            assertThat(ev.getApplied()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("0명은 미충족 — 항목이 적용되지 않는다")
        void zeroCountDoesNotSatisfy() {
            assertThat(run(Map.of("ADDL_BIRTH", 0)).getApplied()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("질문의 answerType 이 COUNT 로 내려간다 — 프론트가 수 입력으로 렌더")
        void questionCarriesAnswerType() {
            given(itemRepository.findByPolicyIdIn(any())).willReturn(List.of(
                    item(NEWBORN, "ADDITIONAL", PreferentialStacking.STACKABLE, "ADDL_BIRTH",
                            "대출접수일 기준 2년 내 추가 출산한 자녀가 몇 명인가요?", 0.2, ASK_COUNT),
                    item(NEWBORN, "ADDITIONAL", PreferentialStacking.STACKABLE, "E_CONTRACT",
                            "부동산 전자계약으로 체결할 예정인가요?", 0.1, ASK_ONLY)));

            List<PreferentialQuestion> qs = service.buildQuestions(List.of(3L), PROFILE_27);

            assertThat(qs).filteredOn(q -> q.getItemKey().equals("ADDL_BIRTH"))
                    .singleElement().satisfies(q -> assertThat(q.getAnswerType()).isEqualTo("COUNT"));
            assertThat(qs).filteredOn(q -> q.getItemKey().equals("E_CONTRACT"))
                    .singleElement().satisfies(q -> assertThat(q.getAnswerType()).isEqualTo("CHECK"));
        }
    }

    // ─── 실데이터 스모크 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("합산 반올림 — 0.1+0.2 부동소수점 잔재가 sum 에 남지 않는다")
    void sumIsRounded() {
        given(itemRepository.findByPolicyIdIn(any())).willReturn(List.of(
                item(BEOTIMMOK, "A", PreferentialStacking.STACKABLE, "E_CONTRACT", "q", 0.1, ASK_ONLY),
                item(BEOTIMMOK, "A", PreferentialStacking.STACKABLE, "RENT_GOOD_PAYER", "q", 0.2, ASK_ONLY)));
        given(capRepository.findByPolicyIdOrderByPriorityAsc(anyLong()))
                .willReturn(List.of(cap(BEOTIMMOK, 9, 0.5, null)));

        PreferentialEvaluation ev = service.evaluate(1L, PROFILE_27, new PlanLoanValues(1, 1),
                Map.of("E_CONTRACT", 1, "RENT_GOOD_PAYER", 1));

        assertThat(ev.getSum()).isEqualTo(0.3);   // 0.30000000000000004 아님
    }
}
