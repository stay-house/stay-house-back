package com.example.stay_house_back.controller;

import com.example.stay_house_back.converter.PlanDtoConverter;
import com.example.stay_house_back.dto.eligibility.EligibilityRequest;
import com.example.stay_house_back.dto.eligibility.EligibilityResponse;
import com.example.stay_house_back.dto.plan.Plan;
import com.example.stay_house_back.dto.ws.PlanDto;
import com.example.stay_house_back.dto.ws.StartRequest;
import com.example.stay_house_back.service.EligibilityFilterService;
import com.example.stay_house_back.service.PlanningEngineService;
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

    @MessageMapping("/start")
    public void start(StartRequest startRequest) {
        String planRequestId = startRequest.getPlanRequestId();
        EligibilityRequest req = startRequest.getEligibilityRequest();
        log.info("[{}] /start 수신 - buildingName: {}", planRequestId, req.getBuildingName());

        send(planRequestId, progress("자격 조건 필터링 중..."));
        EligibilityResponse eligible = eligibilityFilterService.filter(req);

        send(planRequestId, progress("조달 플랜 생성 중..."));
        List<Plan> plans = planningEngineService.generate(eligible, req);

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

    private Map<String, Object> progress(String step) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "PROGRESS");
        msg.put("step", step);
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
