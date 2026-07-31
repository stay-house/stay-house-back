package com.example.stay_house_back.service;

import com.example.stay_house_back.dto.eligibility.EligibilityRequest;
import com.example.stay_house_back.dto.eligibility.EligibilityResponse;
import com.example.stay_house_back.dto.eligibility.EligibleLoanProductDto;
import com.example.stay_house_back.dto.eligibility.EligiblePolicyDto;
import com.example.stay_house_back.dto.plan.Plan;
import com.example.stay_house_back.dto.preferential.PreferentialEvaluation;
import com.example.stay_house_back.dto.preferential.PreferentialProfile;
import com.example.stay_house_back.entity.PolicyRateMatrix;
import com.example.stay_house_back.entity.ProductExclusion;
import com.example.stay_house_back.entity.ProductExclusionId;
import com.example.stay_house_back.entity.enums.ProductType;
import com.example.stay_house_back.repository.PolicyRateMatrixRepository;
import com.example.stay_house_back.repository.ProductExclusionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanningEngineServiceTest {

    // 시그니처 변경(generate 가 req 를 받음)에 맞춘 기본 요청.
    // 보증금 2억 / 자기자본 5천 / 연소득 4천 — 정책 격자 스텁과 함께 쓴다.
    private static final EligibilityRequest REQ = EligibilityRequest.builder()
            .deposit(200_000_000)
            .ownCapital(50_000_000L)
            .annualIncome(40_000_000)
            .build();

    // 우대 0 적용(항등) 스텁 — 우대 계산 자체는 PreferentialRateServiceTest 가 검증한다
    private static final PreferentialProfile PROFILE =
            new PreferentialProfile(30, 40_000_000, 40.0, 200_000_000L);

    @Mock
    private PreferentialRateService preferentialRateService;

    @Mock
    private PolicyRateMatrixRepository policyRateMatrixRepository;

    @Mock
    private ProductExclusionRepository productExclusionRepository;

    @InjectMocks
    private PlanningEngineService planningEngineService;

    @BeforeEach
    void setUp() {
        // 기본적으로 exclusion 없음
        when(productExclusionRepository.findAll()).thenReturn(List.of());
        // 정책 금리 격자는 항상 매칭된다고 가정 — 매칭 실패 시 정책 플랜이
        // 조용히 빠지므로, 그 경로는 별도 테스트로 다룬다
        when(policyRateMatrixRepository.findMatchingRates(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(PolicyRateMatrix.builder().rate(2.5).build()));
        // 우대 0 적용 — applyTo(rate) 가 기본금리를 그대로 돌려준다
        when(preferentialRateService.evaluate(anyLong(), any(), any(), any()))
                .thenReturn(PreferentialEvaluation.builder()
                        .items(List.of()).sum(0).cap(0.5).applied(0).floorRate(1.0).build());
    }

    // ─── 헬퍼 팩토리 메서드 ────────────────────────────────────────────────────

    private EligibleLoanProductDto bankLoan(long id) {
        return EligibleLoanProductDto.builder()
                .id(id)
                .bankName("테스트은행")
                .productName("전세대출" + id)
                .build();
    }

    private EligiblePolicyDto policyLoan(long id) {
        return EligiblePolicyDto.builder()
                .id(id)
                .policyName("버팀목" + id)
                .category("POLICY_LOAN")
                .build();
    }

    private EligiblePolicyDto rentSubsidy(long id) {
        return EligiblePolicyDto.builder()
                .id(id)
                .policyName("월세지원" + id)
                .category("RENT_SUBSIDY")
                .build();
    }

    private ProductExclusion exclusion(ProductType type1, long id1, ProductType type2, long id2) {
        return ProductExclusion.builder()
                .id(ProductExclusionId.builder()
                        .productType(type1).productId(id1)
                        .excludedProductType(type2).excludedProductId(id2)
                        .build())
                .build();
    }

    // ─── 테스트 케이스 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("상품이 하나도 없으면 SELF_FUNDED × NO_SUBSIDY 플랜 1개만 생성된다")
    void emptyEligible_onlySelfFundedNoSubsidy() {
        List<Plan> plans = planningEngineService.generate(EligibilityResponse.empty(), REQ, PROFILE, Map.of());

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getPlanId()).isEqualTo("SELF_FUNDED_NO_SUBSIDY");
    }

    @Test
    @DisplayName("은행 대출 2개만 있으면 (SELF_FUNDED + 은행2개) × NO_SUBSIDY = 3개 플랜")
    void bankLoansOnly_noSubsidy() {
        EligibilityResponse eligible = new EligibilityResponse(
                List.of(bankLoan(1L), bankLoan(2L)),
                List.of()
        );

        List<Plan> plans = planningEngineService.generate(eligible, REQ, PROFILE, Map.of());

        assertThat(plans).hasSize(3);
        assertThat(plans).allMatch(p -> p.getRentSubsidy() == null);
        assertThat(plans).extracting(Plan::getPlanId)
                .containsExactlyInAnyOrder(
                        "SELF_FUNDED_NO_SUBSIDY",
                        "BANK_LOAN_1_NO_SUBSIDY",
                        "BANK_LOAN_2_NO_SUBSIDY"
                );
    }

    @Test
    @DisplayName("정책 대출 1개만 있으면 (SELF_FUNDED + 정책대출1개) × NO_SUBSIDY = 2개 플랜")
    void policyLoanOnly_noSubsidy() {
        EligibilityResponse eligible = new EligibilityResponse(
                List.of(),
                List.of(policyLoan(10L))
        );

        List<Plan> plans = planningEngineService.generate(eligible, REQ, PROFILE, Map.of());

        assertThat(plans).hasSize(2);
        assertThat(plans).extracting(Plan::getPlanId)
                .containsExactlyInAnyOrder(
                        "SELF_FUNDED_NO_SUBSIDY",
                        "POLICY_LOAN_10_NO_SUBSIDY"
                );
    }

    @Test
    @DisplayName("우대금리가 정책 플랜의 금리를 낮춘다 — 격자 2.5% 에 우대 0.5%p 적용 시 2.0%")
    void preferentialLowersPolicyRate() {
        when(preferentialRateService.evaluate(anyLong(), any(), any(), any()))
                .thenReturn(PreferentialEvaluation.builder()
                        .items(List.of()).sum(0.6).cap(0.5).applied(0.5).floorRate(1.0).build());
        EligibilityResponse eligible = new EligibilityResponse(List.of(), List.of(policyLoan(10L)));

        List<Plan> plans = planningEngineService.generate(
                eligible, REQ, PROFILE, Map.of("E_CONTRACT", 1));

        assertThat(plans).filteredOn(p -> p.getPlanId().equals("POLICY_LOAN_10_NO_SUBSIDY"))
                .singleElement()
                .satisfies(p -> assertThat(p.getAnnualRate()).isEqualTo(2.0));
    }

    @Test
    @DisplayName("은행 대출 2개 + 월세지원 1개: 카테시안 곱 = (SELF_FUNDED + 은행2개) × (NO_SUBSIDY + 지원1개) = 6개")
    void bankLoansWithRentSubsidy_cartesianProduct() {
        EligibilityResponse eligible = new EligibilityResponse(
                List.of(bankLoan(1L), bankLoan(2L)),
                List.of(rentSubsidy(20L))
        );

        List<Plan> plans = planningEngineService.generate(eligible, REQ, PROFILE, Map.of());

        assertThat(plans).hasSize(6);
        assertThat(plans).extracting(Plan::getPlanId)
                .containsExactlyInAnyOrder(
                        "SELF_FUNDED_NO_SUBSIDY",
                        "SELF_FUNDED_SUBSIDY_20",
                        "BANK_LOAN_1_NO_SUBSIDY",
                        "BANK_LOAN_1_SUBSIDY_20",
                        "BANK_LOAN_2_NO_SUBSIDY",
                        "BANK_LOAN_2_SUBSIDY_20"
                );
    }

    @Test
    @DisplayName("정책 대출 + 월세지원이 product_exclusion에 등록된 경우 해당 조합만 제거된다")
    void exclusion_removesForbiddenCombination() {
        when(productExclusionRepository.findAll()).thenReturn(
                List.of(exclusion(ProductType.POLICY, 10L, ProductType.POLICY, 20L))
        );

        EligibilityResponse eligible = new EligibilityResponse(
                List.of(),
                List.of(policyLoan(10L), rentSubsidy(20L))
        );

        List<Plan> plans = planningEngineService.generate(eligible, REQ, PROFILE, Map.of());

        // 원래 4개 (SELF_FUNDED × 2 + POLICY_LOAN × 2)에서 POLICY_LOAN_10_SUBSIDY_20 1개 제거
        assertThat(plans).hasSize(3);
        assertThat(plans).extracting(Plan::getPlanId)
                .doesNotContain("POLICY_LOAN_10_SUBSIDY_20")
                .containsExactlyInAnyOrder(
                        "SELF_FUNDED_NO_SUBSIDY",
                        "SELF_FUNDED_SUBSIDY_20",
                        "POLICY_LOAN_10_NO_SUBSIDY"
                );
    }

    @Test
    @DisplayName("product_exclusion은 역방향도 동일하게 필터링된다")
    void exclusion_bidirectional() {
        // exclusion이 반대 방향으로 등록되어 있어도 같은 결과
        when(productExclusionRepository.findAll()).thenReturn(
                List.of(exclusion(ProductType.POLICY, 20L, ProductType.POLICY, 10L))
        );

        EligibilityResponse eligible = new EligibilityResponse(
                List.of(),
                List.of(policyLoan(10L), rentSubsidy(20L))
        );

        List<Plan> plans = planningEngineService.generate(eligible, REQ, PROFILE, Map.of());

        assertThat(plans).hasSize(3);
        assertThat(plans).extracting(Plan::getPlanId)
                .doesNotContain("POLICY_LOAN_10_SUBSIDY_20");
    }

    @Test
    @DisplayName("SELF_FUNDED는 product_exclusion 체크 대상이 아니다")
    void exclusion_doesNotAffectSelfFunded() {
        // 있지도 않은 exclusion이지만, SELF_FUNDED가 영향받지 않음을 명시
        when(productExclusionRepository.findAll()).thenReturn(List.of());

        EligibilityResponse eligible = new EligibilityResponse(
                List.of(),
                List.of(rentSubsidy(20L))
        );

        List<Plan> plans = planningEngineService.generate(eligible, REQ, PROFILE, Map.of());

        assertThat(plans).hasSize(2);
        assertThat(plans).extracting(Plan::getPlanId)
                .containsExactlyInAnyOrder(
                        "SELF_FUNDED_NO_SUBSIDY",
                        "SELF_FUNDED_SUBSIDY_20"
                );
    }

    @Test
    @DisplayName("은행 대출에 대한 exclusion은 은행 대출 + 월세지원 조합만 제거한다")
    void exclusion_bankLoanWithSubsidy() {
        when(productExclusionRepository.findAll()).thenReturn(
                List.of(exclusion(ProductType.BANK_LOAN, 1L, ProductType.POLICY, 20L))
        );

        EligibilityResponse eligible = new EligibilityResponse(
                List.of(bankLoan(1L), bankLoan(2L)),
                List.of(rentSubsidy(20L))
        );

        List<Plan> plans = planningEngineService.generate(eligible, REQ, PROFILE, Map.of());

        // 원래 6개에서 BANK_LOAN_1_SUBSIDY_20 1개 제거 → 5개
        assertThat(plans).hasSize(5);
        assertThat(plans).extracting(Plan::getPlanId)
                .doesNotContain("BANK_LOAN_1_SUBSIDY_20");
    }

    @Test
    @DisplayName("GUARANTEE_FEE_REFUND 카테고리 정책은 대출·월세지원 후보에서 제외되어 플랜에 반영되지 않는다")
    void guaranteeFeeRefund_notIncludedInPlan() {
        EligiblePolicyDto guaranteePolicy = EligiblePolicyDto.builder()
                .id(30L)
                .policyName("보증료지원")
                .category("GUARANTEE_FEE_REFUND")
                .build();

        EligibilityResponse eligible = new EligibilityResponse(
                List.of(),
                List.of(guaranteePolicy)
        );

        List<Plan> plans = planningEngineService.generate(eligible, REQ, PROFILE, Map.of());

        // GUARANTEE_FEE_REFUND는 대출/월세지원 어느 축에도 포함되지 않음
        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getPlanId()).isEqualTo("SELF_FUNDED_NO_SUBSIDY");
    }
}
