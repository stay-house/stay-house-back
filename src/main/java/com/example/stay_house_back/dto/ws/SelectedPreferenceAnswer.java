package com.example.stay_house_back.dto.ws;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SelectedPreferenceAnswer {
    private String questionId;
    private String questionText;
    private String selectedOptionLabel;
}
