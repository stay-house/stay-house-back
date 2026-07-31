package com.example.stay_house_back.converter;

import com.example.stay_house_back.dto.eligibility.EligibilityRequest;
import com.example.stay_house_back.dto.preferential.PreferentialProfile;

public class PreferentialProfileConverter {

    private PreferentialProfileConverter() {}

    public static PreferentialProfile from(EligibilityRequest req) {
        return new PreferentialProfile(
                req.getAge(),
                req.getAnnualIncome(),
                req.getAreaSqm(),
                (long) req.getDeposit()
        );
    }
}
