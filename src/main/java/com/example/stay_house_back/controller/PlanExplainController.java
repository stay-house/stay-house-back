package com.example.stay_house_back.controller;

import com.example.stay_house_back.client.GptClient;
import com.example.stay_house_back.dto.explain.ExplainRequest;
import com.example.stay_house_back.dto.explain.ExplainResponse;
import com.example.stay_house_back.dto.ws.PlanDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 상세 화면의 LLM 추천 설명. 클릭한 플랜 하나만 호출한다 — 5개 전부 미리 돌리면
 * 응답이 그만큼 느려지고 4개는 버려진다. 계산은 하지 않는다 (D18/D20: 숫자는
 * 전부 RESULT 에 실려 온 값이고 LLM 은 문장으로 엮기만 한다).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PlanExplainController {

    private final GptClient gptClient;

    @PostMapping("/api/plan/explain")
    public ExplainResponse explain(@RequestBody ExplainRequest request) {
        PlanDto plan = request.getPlan();
        Map<String, Object> summary = toSummary(plan, request.getBuildingName());
        log.info("설명 요청: planId={} building={}", plan.getPlanId(), request.getBuildingName());
        try {
            JsonNode r = gptClient.generatePlanExplanation(summary);
            List<String> cautions = new ArrayList<>();
            r.path("cautions").forEach(n -> cautions.add(n.asText()));
            List<String> guide = new ArrayList<>();
            r.path("actionGuide").forEach(n -> guide.add(n.asText()));
            return new ExplainResponse(r.path("recommendReason").asText(), cautions, guide, true);
        } catch (Exception e) {
            log.warn("설명 생성 실패, 규칙 기반 폴백: {}", e.getMessage());
            return fallback(plan);
        }
    }

    private Map<String, Object> toSummary(PlanDto p, String buildingName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("buildingName", buildingName);
        m.put("fundingType", p.getFundingType());
        m.put("productName", p.getProductName());
        m.put("bankName", p.getBankName());
        m.put("annualRate", p.getAnnualRate());
        m.put("variableRate", p.isVariableRate());
        m.put("loanAmount", p.getLoanAmount());
        m.put("ownFundingAmount", p.getOwnFundingAmount());
        m.put("shortfall", p.getShortfall());
        m.put("monthlyHousingCost", p.getMonthlyHousingCost());
        m.put("monthlyLoanRepayment", p.getMonthlyLoanRepayment());
        m.put("monthlyRentAfterSubsidy", p.getMonthlyRentAfterSubsidy());
        m.put("maintenanceFee", p.getMaintenanceFee());
        m.put("monthlyIncomeNet", p.getMonthlyIncomeNet());
        m.put("monthlyIncomeEstimated", p.isMonthlyIncomeEstimated());
        m.put("burdenRatio", p.getBurdenRatio());
        m.put("burdenIncrease2pp", p.getBurdenIncrease2pp());
        m.put("scenarios", p.getScenarios());
        m.put("rentSubsidyName", p.getRentSubsidyName());
        m.put("govRentSubsidyAmount", p.getGovRentSubsidyAmount());
        m.put("rentSubsidyBudgetStatus", p.getRentSubsidyBudgetStatus());
        m.put("score", p.getScore());
        return m;
    }

    /** GPT 없이도 화면이 비지 않게 — 플랜 값으로만 만드는 고정 문구 */
    private ExplainResponse fallback(PlanDto p) {
        long manwon = p.getMonthlyHousingCost() != null ? Math.round(p.getMonthlyHousingCost() / 10000.0) : 0;
        String reason = String.format(
                "%s(으)로 보증금 중 %,d만원을 조달하고, 월 예상 주거비는 약 %,d만원입니다. "
                        + "산출된 안정성 점수 기준 상위 플랜입니다.",
                p.getProductName() != null ? p.getProductName() : "자기자본",
                Math.round(p.getLoanAmount() / 10000.0), manwon);
        List<String> cautions = new ArrayList<>();
        if (p.isVariableRate()) cautions.add("변동금리 상품이라 금리가 오르면 월 부담이 늘어날 수 있습니다.");
        if (p.getShortfall() > 0) cautions.add(String.format("자기자금 부족분 %,d만원의 조달 방안이 필요합니다.",
                Math.round(p.getShortfall() / 10000.0)));
        if ("EXHAUSTED".equals(p.getRentSubsidyBudgetStatus()))
            cautions.add("포함된 월세지원은 올해 접수가 마감된 제도입니다 (연 1회 모집).");
        cautions.add("2026년 7월 공시 기준이며, 실제 조건은 은행 심사에 따라 다릅니다.");
        List<String> guide = List.of(
                "임대차 계약 전 해당 상품 취급 기관에 자격·한도를 사전 확인하세요.",
                "신분증·소득 증빙·임대차계약서 등 기본 서류를 준비하세요.",
                "대출 실행일과 잔금일을 맞춰 일정을 계획하세요.");
        return new ExplainResponse(reason, cautions, guide, false);
    }
}
