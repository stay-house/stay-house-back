package com.example.stay_house_back.service;

import com.example.stay_house_back.dto.eligibility.EligibilityResponse;
import com.example.stay_house_back.dto.eligibility.EligibleLoanProductDto;
import com.example.stay_house_back.dto.eligibility.EligiblePolicyDto;
import com.example.stay_house_back.dto.plan.Plan;
import com.example.stay_house_back.entity.ProductExclusion;
import com.example.stay_house_back.entity.ProductExclusionId;
import com.example.stay_house_back.entity.enums.ProductType;
import com.example.stay_house_back.repository.ProductExclusionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningEngineServiceTest {

    @Mock
    private ProductExclusionRepository productExclusionRepository;

    @InjectMocks
    private PlanningEngineService planningEngineService;

    @BeforeEach
    void setUp() {
        // 기본적으로 exclusion 없음
        when(productExclusionRepository.findAll()).thenReturn(List.of());
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
        List<Plan> plans = planningEngineService.generate(EligibilityResponse.empty());

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

        List<Plan> plans = planningEngineService.generate(eligible);

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

        List<Plan> plans = planningEngineService.generate(eligible);

        assertThat(plans).hasSize(2);
        assertThat(plans).extracting(Plan::getPlanId)
                .containsExactlyInAnyOrder(
                        "SELF_FUNDED_NO_SUBSIDY",
                        "POLICY_LOAN_10_NO_SUBSIDY"
                );
    }

    @Test
    @DisplayName("은행 대출 2개 + 월세지원 1개: 카테시안 곱 = (SELF_FUNDED + 은행2개) × (NO_SUBSIDY + 지원1개) = 6개")
    void bankLoansWithRentSubsidy_cartesianProduct() {
        EligibilityResponse eligible = new EligibilityResponse(
                List.of(bankLoan(1L), bankLoan(2L)),
                List.of(rentSubsidy(20L))
        );

        List<Plan> plans = planningEngineService.generate(eligible);

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

        List<Plan> plans = planningEngineService.generate(eligible);

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

        List<Plan> plans = planningEngineService.generate(eligible);

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

        List<Plan> plans = planningEngineService.generate(eligible);

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

        List<Plan> plans = planningEngineService.generate(eligible);

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

        List<Plan> plans = planningEngineService.generate(eligible);

        // GUARANTEE_FEE_REFUND는 대출/월세지원 어느 축에도 포함되지 않음
        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getPlanId()).isEqualTo("SELF_FUNDED_NO_SUBSIDY");
    }
}
