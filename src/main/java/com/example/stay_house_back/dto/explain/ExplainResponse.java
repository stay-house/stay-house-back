package com.example.stay_house_back.dto.explain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExplainResponse {
    private final String recommendReason;
    private final List<String> cautions;
    private final List<String> actionGuide;
    /** false = GPT 실패로 규칙 기반 폴백 문구 — 화면에서 "AI 분석" 라벨을 뺀다 */
    private final boolean aiGenerated;
}
