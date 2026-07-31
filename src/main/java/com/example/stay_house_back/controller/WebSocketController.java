package com.example.stay_house_back.controller;

import com.example.stay_house_back.converter.PlanDtoConverter;
import com.example.stay_house_back.dto.eligibility.EligibilityRequest;
import com.example.stay_house_back.dto.eligibility.EligibilityResponse;
import com.example.stay_house_back.dto.eligibility.EligiblePolicyDto;
import com.example.stay_house_back.dto.plan.Plan;
import com.example.stay_house_back.dto.preferential.PreferentialProfile;
import com.example.stay_house_back.dto.preferential.PreferentialQuestion;
import com.example.stay_house_back.dto.ws.PlanDto;
import com.example.stay_house_back.dto.ws.StartRequest;
import com.example.stay_house_back.service.EligibilityFilterService;
import com.example.stay_house_back.service.PlanningEngineService;
import com.example.stay_house_back.service.PreferentialRateService;
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

    @MessageMapping("/start")
    public void start(StartRequest startRequest) {
        String planRequestId = startRequest.getPlanRequestId();
        EligibilityRequest req = startRequest.getEligibilityRequest();
        log.info("[{}] /start 수신 - buildingName: {}, answers: {}",
                planRequestId, req.getBuildingName(),
                startRequest.getAnswers() != null ? "있음" : "없음");

        send(planRequestId, progress("자격 조건 필터링 중..."));
        EligibilityResponse eligible = eligibilityFilterService.filter(req);

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

        long monthlyRent = req.getMonthlyRent() != null ? req.getMonthlyRent() : 0L;
        long maintenanceFee = req.getMaintenanceFee() != null ? req.getMaintenanceFee() : 0L;
        long monthlyIncomeNet = planningEngineService.resolveMonthlyIncomeNet(req);
        boolean isEstimated = req.getMonthlyIncomeNet() == null;

        List<PlanDto> candidates = plans.stream()
                .map(p -> PlanDtoConverter.toCandidate(p, monthlyRent, maintenanceFee, monthlyIncomeNet, isEstimated))
                .toList();

        log.debug("[{}] 후보 플랜 {}건 전송", planRequestId, candidates.size());
        send(planRequestId, candidates(candidates));
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

    private Map<String, Object> candidates(List<PlanDto> plans) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "CANDIDATES");
        msg.put("plans", plans);
        return msg;
    }

    private void send(String sessionId, Object payload) {
        messagingTemplate.convertAndSend("/topic/plan/" + sessionId, payload);
    }
}
