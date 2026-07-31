package com.example.stay_house_back.service;

import com.example.stay_house_back.client.GptClient;
import com.example.stay_house_back.dto.plan.Plan;
import com.example.stay_house_back.dto.ws.PreferenceQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceQuestionService {

    private final GptClient gptClient;

    public List<PreferenceQuestion> generate(List<Plan> plans) {
        if (plans.isEmpty()) return List.of();

        List<Map<String, Object>> summaries = plans.stream()
                .map(this::toSummary)
                .toList();

        log.info("===== GPT 요청 플랜 요약 {}건 =====", summaries.size());
        summaries.forEach(s -> log.info("  {}", s));

        try {
            List<PreferenceQuestion> questions = gptClient.generatePreferenceQuestions(summaries);
            log.info("===== GPT 응답 질문 {}건 =====", questions.size());
            questions.forEach(q -> {
                log.info("  questionId={} text={}", q.getQuestionId(), q.getText());
                q.getOptions().forEach(o -> log.info("    label={} boostPlanIds={}", o.getLabel(), o.getBoostPlanIds()));
            });
            return questions;
        } catch (Exception e) {
            log.warn("선호도 질문 생성 실패, 빈 목록으로 폴백: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> toSummary(Plan plan) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("planId", plan.getPlanId());

        // 대출 조건
        summary.put("annualRate", plan.getAnnualRate());
        summary.put("variableRate", plan.isVariableRate());
        summary.put("loanAmount", plan.getLoanAmount());
        summary.put("shortfall", plan.getShortfall());

        // 상품 정보
        summary.put("category", plan.getFundingSource().getType().name());
        switch (plan.getFundingSource().getType()) {
            case BANK_LOAN -> {
                summary.put("bankName", plan.getFundingSource().getBankLoan().getBankName());
                summary.put("joinWay", plan.getFundingSource().getBankLoan().getJoinWay());
            }
            case POLICY_LOAN -> {
                summary.put("policyName", plan.getFundingSource().getPolicyLoan().getPolicyName());
            }
            case SELF_FUNDED -> {}
        }

        // 월세 지원
        if (plan.getRentSubsidy() != null) {
            summary.put("rentSubsidyMonthlyAmount", plan.getRentSubsidy().getMonthlyAmount());
        }

        return summary;
    }
}
