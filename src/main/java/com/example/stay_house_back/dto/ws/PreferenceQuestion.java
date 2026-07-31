package com.example.stay_house_back.dto.ws;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PreferenceQuestion {
    private String questionId;
    private String text;
    private List<PreferenceOption> options;

    public PreferenceQuestion(String questionId, String text, List<PreferenceOption> options) {
        this.questionId = questionId;
        this.text = text;
        this.options = options;
    }
}
