package com.example.stay_house_back.dto.ws;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PreferenceOption {
    private String label;
    private List<String> boostPlanIds;

    public PreferenceOption(String label, List<String> boostPlanIds) {
        this.label = label;
        this.boostPlanIds = boostPlanIds;
    }
}
