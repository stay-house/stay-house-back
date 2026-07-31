package com.example.stay_house_back.controller;

import com.example.stay_house_back.converter.PlanDtoConverter;
import com.example.stay_house_back.dto.eligibility.EligibilityRequest;
import com.example.stay_house_back.dto.eligibility.EligibilityResponse;
import com.example.stay_house_back.dto.eligibility.EligiblePolicyDto;
import com.example.stay_house_back.dto.plan.Plan;
import com.example.stay_house_back.dto.preferential.PreferentialProfile;
import com.example.stay_house_back.dto.preferential.PreferentialQuestion;
import com.example.stay_house_back.dto.ws.AnswerMessage;
import com.example.stay_house_back.dto.ws.PlanDto;
import com.example.stay_house_back.dto.ws.PreferenceQuestion;
import com.example.stay_house_back.dto.ws.StartRequest;
import com.example.stay_house_back.service.EligibilityFilterService;
import com.example.stay_house_back.service.PlanningEngineService;
import com.example.stay_house_back.service.PlanScoringService;
import com.example.stay_house_back.service.PreferentialRateService;
import com.example.stay_house_back.service.PreferenceQuestionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final EligibilityFilterService eligibilityFilterService;
    private final PlanningEngineService planningEngineService;
    private final PreferentialRateService preferentialRateService;
    private final PreferenceQuestionService preferenceQuestionService;
    private final PlanScoringService planScoringService;

    @MessageMapping("/start")
    public void start(StartRequest startRequest) {
        String planRequestId = startRequest.getPlanRequestId();
        EligibilityRequest req = startRequest.getEligibilityRequest();
        log.info("[{}] /start 수신 - buildingName: {}, answers: {}",
                planRequestId, req.getBuildingName(),
                startRequest.getAnswers() != null ? "있음" : "없음");

        send(planRequestId, progress("자격 조건 필터링 중..."));
        EligibilityResponse eligible = eligibilityFilterService.filter(req);
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        log.info("[{}] ===== 필터링 결과 =====", planRequestId);
        log.info("[{}] 은행 대출 상품 {}건:", planRequestId, eligible.getLoanProducts().size());
        eligible.getLoanProducts().forEach(p -> log.info("  bankName={} productName={} rateMin={} rateMax={} maxAmount={} ltvRatio={} isYouth={} guaranteeAgency={} joinWay={}",
                p.getBankName(), p.getProductName(), p.getRateMin(), p.getRateMax(),
                p.getMaxAmount(), p.getLtvRatio(), p.getIsYouth(), p.getGuaranteeAgency(), p.getJoinWay()));
        log.info("[{}] 정책 {}건:", planRequestId, eligible.getPolicies().size());
        eligible.getPolicies().forEach(p -> log.info("  policyName={} category={} operatingAgency={} rateMin={} rateMax={} loanLmtMax={} ltvRatio={} monthlyAmount={}",
                p.getPolicyName(), p.getCategory(), p.getOperatingAgency(),
                p.getRateMin(), p.getRateMax(), p.getLoanLmtMax(), p.getLtvRatio(), p.getMonthlyAmount()));

        // 우대금리 질문 — 정책 후보가 있고 아직 답변 전이면 여기서 멈추고 대기.
        // 클라이언트가 같은 요청에 answers 를 붙여 다시 보낸다 (무상태 왕복).
        // 질문이 0건이면(정책 없음 / PROFILE 전부 탈락) 바로 플랜 생성으로 간다.
        PreferentialProfile profile = toPreferentialProfile(req);
        if (startRequest.getAnswers() == null) {
            List<Long> policyIds = eligible.getPolicies().stream()
                    .map(EligiblePolicyDto::getId)
                    .toList();
            List<PreferentialQuestion> questions =
                    preferentialRateService.buildQuestions(policyIds, profile);
            if (!questions.isEmpty()) {
                log.debug("[{}] 우대금리 질문 {}건 전송, 답변 대기", planRequestId, questions.size());
                send(planRequestId, needMoreInfo(questions));
                return;
            }
        }

        send(planRequestId, progress("조달 플랜 생성 중..."));
        Map<String, Integer> answers =
                startRequest.getAnswers() != null ? startRequest.getAnswers() : Map.of();
        List<Plan> plans = planningEngineService.generate(eligible, req, profile, answers);

        log.info("[{}] ===== 생성된 플랜 {}건 =====", planRequestId, plans.size());
        plans.forEach(p -> {
            String productLabel = switch (p.getFundingSource().getType()) {
                case BANK_LOAN -> "은행=" + p.getFundingSource().getBankLoan().getBankName() + "/" + p.getFundingSource().getBankLoan().getProductName();
                case POLICY_LOAN -> "정책=" + p.getFundingSource().getPolicyLoan().getPolicyName();
                case SELF_FUNDED -> "자기자본";
            };
            String subsidyLabel = p.getRentSubsidy() != null ? p.getRentSubsidy().getPolicyName() + "(" + p.getRentSubsidy().getMonthlyAmount() + "원/월)" : "없음";
            log.info("  planId={} {} | 금리={}% ({}) | 대출={}원 capAmount={}원 부족분={}원 | 월세지원={}",
                    p.getPlanId(), productLabel, p.getAnnualRate(),
                    p.isVariableRate() ? "변동" : "고정",
                    p.getLoanAmount(), p.getCapAmount(), p.getShortfall(), subsidyLabel);
        });

        long monthlyRent = req.getMonthlyRent() != null ? req.getMonthlyRent() : 0L;
        long maintenanceFee = req.getMaintenanceFee() != null ? req.getMaintenanceFee() : 0L;
        long monthlyIncomeNet = planningEngineService.resolveMonthlyIncomeNet(req);
        boolean isEstimated = req.getMonthlyIncomeNet() == null;

        List<PlanDto> candidates = plans.stream()
                .map(p -> PlanDtoConverter.toCandidate(p, req.getDeposit(), monthlyRent, maintenanceFee, monthlyIncomeNet, isEstimated))
                .toList();

        send(planRequestId, progress("선호도 질문 생성 중..."));
        List<PreferenceQuestion> preferenceQuestions = preferenceQuestionService.generate(plans);

        log.info("[{}] 후보 플랜 {}건, 선호도 질문 {}건 전송", planRequestId, candidates.size(), preferenceQuestions.size());
        send(planRequestId, candidates(candidates, preferenceQuestions));
    }

    private PreferentialProfile toPreferentialProfile(EligibilityRequest req) {
        return new PreferentialProfile(
                req.getAge(),
                req.getAnnualIncome(),
                req.getAreaSqm(),
                (long) req.getDeposit());
    }

    private Map<String, Object> progress(String step) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "PROGRESS");
        msg.put("step", step);
        return msg;
    }

    private Map<String, Object> needMoreInfo(List<PreferentialQuestion> questions) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "NEED_MORE_INFO");
        msg.put("questions", questions);
        return msg;
    }

    private Map<String, Object> candidates(List<PlanDto> plans, List<PreferenceQuestion> preferenceQuestions) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "CANDIDATES");
        msg.put("plans", plans);
        msg.put("preferenceQuestions", preferenceQuestions);
        return msg;
    }

    @MessageMapping("/answer")
    public void answer(AnswerMessage msg) {
        String planRequestId = msg.getPlanRequestId();
        log.info("[{}] ===== /answer 수신 =====", planRequestId);
        log.info("[{}] boostedPlanIds: {}", planRequestId, msg.getBoostedPlanIds());
        log.info("[{}] 에코된 candidates {}건:", planRequestId, msg.getCandidates().size());
        msg.getCandidates().forEach(p -> log.info("  planId={} productName={} fundingType={} 금리={}% ({}) 대출={}원 부족분={}원",
                p.getPlanId(), p.getProductName(), p.getFundingType(),
                p.getAnnualRate(), p.isVariableRate() ? "변동" : "고정",
                p.getLoanAmount(), p.getShortfall()));

        List<PlanDto> ranked = planScoringService.rank(msg.getCandidates(), msg.getBoostedPlanIds());

        log.info("[{}] ===== 순위 결과 {}건 =====", planRequestId, ranked.size());
        for (int i = 0; i < ranked.size(); i++) {
            PlanDto p = ranked.get(i);
            log.info("  [{}위] planId={} productName={} fundingType={} category={}",
                    i + 1, p.getPlanId(), p.getProductName(), p.getFundingType(), p.getCategory());
            log.info("       금리={}% ({}) 대출={}원 capAmount={}원 부족분={}원",
                    p.getAnnualRate(), p.isVariableRate() ? "변동" : "고정",
                    p.getLoanAmount(), p.getCapAmount(), p.getShortfall());
            log.info("       월세지원={} govRentSubsidyAmount={}원",
                    p.getRentSubsidyName(), p.getGovRentSubsidyAmount());
            log.info("       burdenRatio={} burdenIncrease2pp={}",
                    p.getBurdenRatio(), p.getBurdenIncrease2pp());
            if (p.getScenarios() != null) {
                p.getScenarios().forEach(s -> log.info("       시나리오 +{}%p | 월주거비={}원 대출상환={}원 순월세={}원 부담률={}",
                        s.getRateOffsetPercent(), s.getTotalMonthlyHousingCost(),
                        s.getMonthlyLoanRepayment(), s.getMonthlyRentAfterSubsidy(), s.getBurdenRatio()));
            }
        }

        log.info("[{}] ===== 프론트 전송 payload =====", planRequestId);
        for (int i = 0; i < ranked.size(); i++) {
            PlanDto p = ranked.get(i);
            log.info("[{}] {}위 score={} oneLiner={}", planRequestId, i + 1, p.getScore(), p.getOneLiner());
            log.info("     planId={} productName={} fundingType={} category={}", p.getPlanId(), p.getProductName(), p.getFundingType(), p.getCategory());
            log.info("     annualRate={}% variableRate={} loanAmount={}원 shortfall={}원", p.getAnnualRate(), p.isVariableRate(), p.getLoanAmount(), p.getShortfall());
            log.info("     burdenRatio={} burdenIncrease2pp={}", p.getBurdenRatio(), p.getBurdenIncrease2pp());
            log.info("     rentSubsidyName={} govRentSubsidyAmount={}원", p.getRentSubsidyName(), p.getGovRentSubsidyAmount());
            if (p.getScenarios() != null) {
                p.getScenarios().forEach(s -> log.info("     시나리오 +{}%p 월주거비={}원 부담률={}", s.getRateOffsetPercent(), s.getTotalMonthlyHousingCost(), s.getBurdenRatio()));
            }
        }
        send(planRequestId, result(ranked, msg.getPreferenceAnswers()));
    }

    private Map<String, Object> result(List<PlanDto> plans, List<com.example.stay_house_back.dto.ws.SelectedPreferenceAnswer> preferenceAnswers) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "RESULT");
        msg.put("plans", plans);
        msg.put("preferenceAnswers", preferenceAnswers != null ? preferenceAnswers : List.of());
        return msg;
    }

    private void send(String sessionId, Object payload) {
        messagingTemplate.convertAndSend("/topic/plan/" + sessionId, payload);
    }
}
