package com.example.stay_house_back.controller;

import com.example.stay_house_back.dto.eligibility.EligibilityRequest;
import com.example.stay_house_back.dto.eligibility.EligibilityResponse;
import com.example.stay_house_back.service.EligibilityFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eligibility")
@RequiredArgsConstructor
public class EligibilityController {

    private final EligibilityFilterService eligibilityFilterService;

    @PostMapping("/check")
    public EligibilityResponse check(@RequestBody EligibilityRequest request) {
        return eligibilityFilterService.filter(request);
    }
}
