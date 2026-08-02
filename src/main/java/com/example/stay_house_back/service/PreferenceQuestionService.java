package com.example.stay_house_back.service;

import com.example.stay_house_back.client.GptClient;
import com.example.stay_house_back.dto.plan.Plan;
import com.example.stay_house_back.dto.plan.FundingSourceType;
import com.example.stay_house_back.dto.ws.PreferenceOption;
import com.example.stay_house_back.dto.ws.PreferenceQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceQuestionService {

    private final GptClient gptClient;

    public List<PreferenceQuestion> generate(List<Plan> plans) {
        if (plans.isEmpty()) return List.of();

        // 상품 유형(정부/은행) 질문은 GPT 에 맡기지 않는다 — 규칙으로 완전히 풀리는
        // 질문이라 Java 가 만든다. GPT 는 "섞여 있을 때만"이라는 프롬프트 지시를 어기고
        // 정책 플랜이 0건인데도 이 질문을 만들면서 "정부 지원"을 월세지원이 붙은
        // 은행 플랜으로 매핑한 적이 있다 (정부지원 선택 → 은행이 가산점을 받는 사고).
        List<PreferenceQuestion> questions = new ArrayList<>();
        List<String> policyIds = idsOf(plans, FundingSourceType.POLICY_LOAN);
        List<String> bankIds = idsOf(plans, FundingSourceType.BANK_LOAN);
        if (!policyIds.isEmpty() && !bankIds.isEmpty()) {
            questions.add(new PreferenceQuestion("funding-type",
                    "정부 지원 대출과 은행 대출 중 어떤 것을 선호하시나요?",
                    List.of(new PreferenceOption("정부 지원 대출", policyIds),
                            new PreferenceOption("은행 대출", bankIds),
                            new PreferenceOption("상관없음", List.of()))));
        }

        List<Map<String, Object>> summaries = plans.stream()
                .map(this::toSummary)
                .toList();

        log.info("===== GPT 요청 플랜 요약 {}건 =====", summaries.size());
        summaries.forEach(s -> log.info("  {}", s));

        try {
            List<PreferenceQuestion> gptQuestions =
                    sanitize(gptClient.generatePreferenceQuestions(summaries), plans);
            questions.addAll(gptQuestions);
        } catch (Exception e) {
            log.warn("선호도 질문 생성 실패, 규칙 질문만 사용: {}", e.getMessage());
        }

        List<PreferenceQuestion> result = questions.stream().limit(3).toList();
        log.info("===== 선호도 질문 {}건 (규칙 {} + GPT) =====",
                result.size(), policyIds.isEmpty() || bankIds.isEmpty() ? 0 : 1);
        result.forEach(q -> {
            log.info("  questionId={} text={}", q.getQuestionId(), q.getText());
            q.getOptions().forEach(o -> log.info("    label={} boostPlanIds={}", o.getLabel(), o.getBoostPlanIds()));
        });
        return result;
    }

    private List<String> idsOf(List<Plan> plans, FundingSourceType type) {
        return plans.stream()
                .filter(p -> p.getFundingSource().getType() == type)
                .map(Plan::getPlanId)
                .toList();
    }

    /**
     * GPT 출력 검증 — 전부 결정론적 규칙이다.
     * ① 유형 질문은 Java 가 이미 만들었으니 GPT 가 또 만들면 버린다
     * ② 존재하지 않는 planId 는 잘라낸다 (환각 가드)
     * ③ 정리 후 가산점을 주는 선택지가 하나도 없는 질문은 버린다
     */
    private List<PreferenceQuestion> sanitize(List<PreferenceQuestion> raw, List<Plan> plans) {
        Set<String> validIds = plans.stream().map(Plan::getPlanId).collect(Collectors.toSet());
        List<PreferenceQuestion> cleaned = new ArrayList<>();
        for (PreferenceQuestion q : raw) {
            String text = q.getText() == null ? "" : q.getText();
            if (text.contains("정부 지원") || text.contains("정부지원")) {
                log.info("  [검증 탈락] 유형 질문 중복/부적합: {}", text);
                continue;
            }
            List<PreferenceOption> options = q.getOptions().stream()
                    .map(o -> new PreferenceOption(o.getLabel(),
                            o.getBoostPlanIds() == null ? List.<String>of()
                                    : o.getBoostPlanIds().stream().filter(validIds::contains).toList()))
                    .toList();
            boolean anyBoost = options.stream().anyMatch(o -> !o.getBoostPlanIds().isEmpty());
            if (!anyBoost || options.size() < 2) {
                log.info("  [검증 탈락] 유효한 가산 선택지 없음: {}", text);
                continue;
            }
            cleaned.add(new PreferenceQuestion(q.getQuestionId(), text, options));
        }
        return cleaned;
    }

    private Map<String, Object> toSummary(Plan plan) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("planId", plan.getPlanId());

        // 대출 계산 결과
        summary.put("annualRate", plan.getAnnualRate());
        summary.put("variableRate", plan.isVariableRate());
        summary.put("loanAmount", plan.getLoanAmount());
        summary.put("capAmount", plan.getCapAmount());
        summary.put("shortfall", plan.getShortfall());

        // 상품 정보 전체
        summary.put("fundingType", plan.getFundingSource().getType().name());
        switch (plan.getFundingSource().getType()) {
            case BANK_LOAN -> {
                var b = plan.getFundingSource().getBankLoan();
                summary.put("bankName", b.getBankName());
                summary.put("productName", b.getProductName());
                summary.put("joinWay", b.getJoinWay());
                summary.put("isYouth", b.getIsYouth());
                summary.put("guaranteeAgency", b.getGuaranteeAgency());
                summary.put("rateMin", b.getRateMin());
                summary.put("rateMax", b.getRateMax());
                summary.put("housingTarget", b.getHousingTarget());
            }
            case POLICY_LOAN -> {
                var p = plan.getFundingSource().getPolicyLoan();
                summary.put("policyName", p.getPolicyName());
                summary.put("operatingAgency", p.getOperatingAgency());
                summary.put("rateMin", p.getRateMin());
                summary.put("rateMax", p.getRateMax());
                summary.put("loanLmtMax", p.getLoanLmtMax());
            }
            case SELF_FUNDED -> {}
        }

        // 월세 지원 전체
        if (plan.getRentSubsidy() != null) {
            var rs = plan.getRentSubsidy();
            summary.put("rentSubsidyName", rs.getPolicyName());
            summary.put("rentSubsidyMonthlyAmount", rs.getMonthlyAmount());
            summary.put("rentSubsidyBudgetStatus", rs.getBudgetStatus());
        }

        return summary;
    }
}
