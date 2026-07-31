package com.example.stay_house_back.dto.ws;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AnswerMessage {
    private String planRequestId;
    private List<PlanDto> candidates;                       // CANDIDATES 에서 받은 플랜 목록 에코
    private List<String> boostedPlanIds;                    // 선택한 선택지의 boostPlanIds 합산
    private List<SelectedPreferenceAnswer> preferenceAnswers; // 질문별 선택한 선택지 (RESULT에 포함)
}
