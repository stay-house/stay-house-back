package com.example.stay_house_back.service;

import com.example.stay_house_back.dto.eligibility.EligiblePolicyDto;
import com.example.stay_house_back.dto.eligibility.EligibilityResponse;
import com.example.stay_house_back.dto.plan.FundingSource;
import com.example.stay_house_back.dto.plan.Plan;
import com.example.stay_house_back.entity.ProductExclusion;
import com.example.stay_house_back.entity.enums.ProductType;
import com.example.stay_house_back.repository.ProductExclusionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanningEngineService {

    private final ProductExclusionRepository productExclusionRepository;

    // 자격 필터링 결과로부터 플랜 조합을 생성한다.
    public List<Plan> generate(EligibilityResponse eligible) {
        // 1. 정책을 역할별로 분류
        List<EligiblePolicyDto> policyLoans = eligible.getPolicies().stream()
                .filter(p -> "POLICY_LOAN".equals(p.getCategory()))
                .toList();

        List<EligiblePolicyDto> rentSubsidies = eligible.getPolicies().stream()
                .filter(p -> "RENT_SUBSIDY".equals(p.getCategory()))
                .toList();

        // 2. 대출 방법 후보 목록 (택1)
        List<FundingSource> fundingSources = new ArrayList<>();
        fundingSources.add(FundingSource.selfFunded());
        eligible.getLoanProducts().forEach(lp -> fundingSources.add(FundingSource.ofBankLoan(lp)));
        policyLoans.forEach(pl -> fundingSources.add(FundingSource.ofPolicyLoan(pl)));

        // 3. 월세지원 후보 목록 (독립축). null = 지원 없음
        List<EligiblePolicyDto> subsidyOptions = new ArrayList<>();
        subsidyOptions.add(null);
        subsidyOptions.addAll(rentSubsidies);

        // 4. 카테시안 곱으로 플랜 생성
        List<Plan> plans = new ArrayList<>();
        for (FundingSource fs : fundingSources) {
            for (EligiblePolicyDto subsidy : subsidyOptions) {
                plans.add(Plan.of(fs, subsidy));
            }
        }

        // 5. product_exclusion에 등록된 상호배타 조합 제거
        Set<String> exclusions = loadExclusionKeys();
        plans.removeIf(plan -> isExcluded(plan, exclusions));

        return plans;
    }

    // ─── 상호배타 체크 ────────────────────────────────────────────────────────

    /**
     * product_exclusion 전체를 "TYPE:ID->EXCTYPE:EXCID" 문자열 Set으로 로드.
     * 데이터 건수가 적어 전체 로드 후 Java에서 체크한다.
     */
    private Set<String> loadExclusionKeys() {
        return productExclusionRepository.findAll().stream()
                .map(this::toExclusionKey)
                .collect(Collectors.toSet());
    }

    private String toExclusionKey(ProductExclusion e) {
        return e.getId().getProductType().name() + ":" + e.getId().getProductId()
                + "->" + e.getId().getExcludedProductType().name() + ":" + e.getId().getExcludedProductId();
    }

    /**
     * 플랜의 (대출방법, 월세지원) 쌍이 product_exclusion에 등록되어 있는지 확인.
     * 방향 무관하게 양방향 모두 체크한다.
     */
    private boolean isExcluded(Plan plan, Set<String> exclusions) {
        ProductType fundingType = plan.getFundingSource().getProductType();
        Long fundingId = plan.getFundingSource().getProductId();
        EligiblePolicyDto subsidy = plan.getRentSubsidy();

        // SELF_FUNDED 또는 월세지원 없음이면 배타 체크 불필요
        if (fundingId == null || subsidy == null) return false;

        String key1 = fundingType.name() + ":" + fundingId + "->POLICY:" + subsidy.getId();
        String key2 = "POLICY:" + subsidy.getId() + "->" + fundingType.name() + ":" + fundingId;

        return exclusions.contains(key1) || exclusions.contains(key2);
    }
}
