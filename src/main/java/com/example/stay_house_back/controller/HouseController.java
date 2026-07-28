package com.example.stay_house_back.controller;

import com.example.stay_house_back.dto.FloorTypeResponse;
import com.example.stay_house_back.dto.HouseSearchResponse;
import com.example.stay_house_back.service.HouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;

    @GetMapping("/search")
    public List<HouseSearchResponse> search(@RequestParam String query) {
        return houseService.searchAddress(query);
    }

    @GetMapping("/floor-types")
    public List<FloorTypeResponse> floorTypes(
            @RequestParam String sigunguCd,
            @RequestParam String bjdongCd,
            @RequestParam String bun,
            @RequestParam(defaultValue = "0") String ji) {
        return houseService.getFloorTypes(sigunguCd, bjdongCd, bun, ji);
    }
}
