package com.example.stay_house_back.service;

import com.example.stay_house_back.dto.eligibility.EligibilityRequest;
import com.example.stay_house_back.dto.eligibility.EligibilityResponse;
import com.example.stay_house_back.dto.eligibility.EmploymentType;
import com.example.stay_house_back.dto.simulator.enums.HousingType;
import com.example.stay_house_back.entity.EligibilityCondition;
import com.example.stay_house_back.entity.Policy;
import com.example.stay_house_back.entity.enums.LifeEvent;
import com.example.stay_house_back.entity.enums.PolicyCategory;
import com.example.stay_house_back.repository.EligibilityConditionRepository;
import com.example.stay_house_back.repository.LoanProductRateOptionRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * 자격 필터의 두 게이트 — 순자산, 생애사건.
 *
 * <p>두 축은 성격이 다르다. 순자산은 나이·소득과 같은 "확인 필요" 축이라 미입력을
 * 통과시키고, 생애사건은 그 제도의 존재 이유라 미입력을 통과시키지 않는다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EligibilityFilterServiceTest {

    @Mock
    private EligibilityConditionRepository eligibilityConditionRepository;

    @Mock
    private LoanProductRateOptionRepository loanProductRateOptionRepository;

    @InjectMocks
    private EligibilityFilterService service;

    @BeforeEach
    void noLoanProducts() {
        // 이 테스트는 정책만 본다. 은행 전세 상품은 비워 둔다.
        given(eligibilityConditionRepository.findByLoanProductIsNotNull()).willReturn(List.of());
    }

    // ── 픽스처 ──────────────────────────────────────────────────────────────

    /** 전세피해 임차인 버팀목전세자금을 본뜬 정책. 순자산 5.11억 상한. */
    private void givenPolicy(LifeEvent event, Integer assetLimit) {
        Policy policy = Policy.builder()
                .id(6L)
                .policyNm("전세피해 임차인 버팀목전세자금")
                .sourceCode("FP05021201")
                .category(PolicyCategory.POLICY_LOAN)
                .build();

        EligibilityCondition ec = EligibilityCondition.builder()
                .policy(policy)
                .incomeMax(130_000_000)
                .assetLimit(assetLimit)
                .houseOwnerType("무주택")
                .requiredLifeEvent(event)
                .build();

        given(eligibilityConditionRepository.findByPolicyIsNotNull()).willReturn(List.of(ec));
    }

    /** 자격을 충족하는 기본 요청. 생애사건·순자산만 테스트마다 갈아 끼운다. */
    private EligibilityRequest.EligibilityRequestBuilder baseRequest() {
        return EligibilityRequest.builder()
                .korean(true)
                .age(30)
                .employmentType(EmploymentType.EMPLOYED)
                .annualIncome(40_000_000)
                .noHouse(true)
                .housingType(HousingType.JEONSE)
                .address("서울특별시 관악구")
                .deposit(200_000_000)
                .areaSqm(40.0);
    }

    private List<String> policyNamesOf(EligibilityRequest req) {
        EligibilityResponse res = service.filter(req);
        return res.getPolicies().stream().map(p -> p.getPolicyName()).toList();
    }

    // ── 생애사건 ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("생애사건 게이트")
    class LifeEventGate {

        @Test
        @DisplayName("전세피해를 겪지 않았으면 전세피해 대출이 후보에서 빠진다")
        void excludesWhenNotVictim() {
            givenPolicy(LifeEvent.JEONSE_VICTIM, null);

            assertThat(policyNamesOf(baseRequest().jeonseVictim(false).build())).isEmpty();
        }

        @Test
        @DisplayName("모름(null)은 통과시키지 않는다 — 그 제도의 존재 이유이기 때문")
        void excludesWhenUnknown() {
            givenPolicy(LifeEvent.JEONSE_VICTIM, null);

            // jeonseVictim 을 아예 넣지 않은 요청
            assertThat(policyNamesOf(baseRequest().build())).isEmpty();
        }

        @Test
        @DisplayName("전세피해자면 후보에 남는다")
        void includesWhenVictim() {
            givenPolicy(LifeEvent.JEONSE_VICTIM, null);

            assertThat(policyNamesOf(baseRequest().jeonseVictim(true).build()))
                    .containsExactly("전세피해 임차인 버팀목전세자금");
        }

        @Test
        @DisplayName("NONE 인 제도는 생애사건을 묻지 않고 통과한다")
        void passesWhenNoneRequired() {
            givenPolicy(LifeEvent.NONE, null);

            assertThat(policyNamesOf(baseRequest().build())).hasSize(1);
        }

        @Test
        @DisplayName("null(은행 전세 상품처럼 이 축이 없는 경우)도 통과한다")
        void passesWhenColumnNull() {
            givenPolicy(null, null);

            assertThat(policyNamesOf(baseRequest().build())).hasSize(1);
        }

        @Test
        @DisplayName("요구 사건과 다른 사건에 해당해도 통과시키지 않는다")
        void doesNotCrossMatch() {
            givenPolicy(LifeEvent.NEWBORN, null);

            assertThat(policyNamesOf(baseRequest().jeonseVictim(true).newlywed(true).build()))
                    .isEmpty();
        }
    }

    // ── 순자산 ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("순자산 게이트")
    class AssetGate {

        @Test
        @DisplayName("상한을 넘으면 빠진다")
        void excludesOverLimit() {
            givenPolicy(LifeEvent.NONE, 511_000_000);

            assertThat(policyNamesOf(baseRequest().netAsset(600_000_000).build())).isEmpty();
        }

        @Test
        @DisplayName("상한과 같으면 통과한다 (이하 조건)")
        void includesAtLimit() {
            givenPolicy(LifeEvent.NONE, 511_000_000);

            assertThat(policyNamesOf(baseRequest().netAsset(511_000_000).build())).hasSize(1);
        }

        @Test
        @DisplayName("미입력(null)은 통과시킨다 — 나이·소득과 같은 '확인 필요' 축")
        void passesWhenUnknown() {
            givenPolicy(LifeEvent.NONE, 511_000_000);

            assertThat(policyNamesOf(baseRequest().build())).hasSize(1);
        }

        @Test
        @DisplayName("상한이 없는 제도는 순자산을 보지 않는다")
        void ignoredWhenNoLimit() {
            givenPolicy(LifeEvent.NONE, null);

            assertThat(policyNamesOf(baseRequest().netAsset(2_000_000_000).build())).hasSize(1);
        }
    }
}
