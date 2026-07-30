package com.example.stay_house_back.dto.eligibility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class EligibilityResponse {
    private final List<EligibleLoanProductDto> loanProducts;
    private final List<EligiblePolicyDto> policies;

    public static EligibilityResponse empty() {
        return new EligibilityResponse(Collections.emptyList(), Collections.emptyList());
    }
}
