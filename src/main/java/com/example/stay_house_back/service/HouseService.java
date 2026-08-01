package com.example.stay_house_back.service;

import com.example.stay_house_back.client.BuildingRegistryClient;
import com.example.stay_house_back.client.KakaoAddressClient;
import com.example.stay_house_back.dto.FloorTypeResponse;
import com.example.stay_house_back.dto.HouseSearchResponse;
import com.example.stay_house_back.dto.building.BuildingRegistryResponse;
import com.example.stay_house_back.dto.kakao.KakaoAddressResponse;
import com.example.stay_house_back.dto.kakao.KakaoAddressResponse.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HouseService {

    private final KakaoAddressClient kakaoAddressClient;
    private final BuildingRegistryClient buildingRegistryClient;

    public List<HouseSearchResponse> searchAddress(String query) {
        KakaoAddressResponse response = kakaoAddressClient.searchAddress(query);

        return response.documents().stream()
                .filter(doc -> doc.address() != null)
                .map(this::toHouseSearchResponse)
                .toList();
    }

    public List<FloorTypeResponse> getFloorTypes(
            String sigunguCd, String bjdongCd, String bun, String ji) {

        BuildingRegistryResponse response =
                buildingRegistryClient.getExposedAreaInfo(sigunguCd, bjdongCd, bun, ji);

        if (response == null
                || response.body() == null
                || response.body().items() == null
                || response.body().items().item() == null) {
            return List.of();
        }

        List<BuildingRegistryResponse.Item> items = response.body().items().item();

        Map<String, Double> exclusiveByUnit = items.stream()
                .filter(i -> "전유".equals(i.exposPubuseGbCdNm())
                          && "주건축물".equals(i.mainAtchGbCdNm())
                          && i.area() != null && i.area() > 0)
                .collect(Collectors.groupingBy(
                        i -> i.dongNm() + "|" + i.hoNm(),
                        Collectors.summingDouble(BuildingRegistryResponse.Item::area)));

        Map<String, Double> commonByUnit = items.stream()
                .filter(i -> "공용".equals(i.exposPubuseGbCdNm())
                          && "주건축물".equals(i.mainAtchGbCdNm())
                          && isResidentialCommon(i.etcPurps())
                          && i.area() != null && i.area() > 0)
                .collect(Collectors.groupingBy(
                        i -> i.dongNm() + "|" + i.hoNm(),
                        Collectors.summingDouble(BuildingRegistryResponse.Item::area)));

        return exclusiveByUnit.entrySet().stream()
                .map(e -> {
                    double exclusive = e.getValue();
                    double common = commonByUnit.getOrDefault(e.getKey(), 0.0);
                    double supply = exclusive + common;
                    return new FloorTypeResponse(
                            Math.round(exclusive * 100.0) / 100.0,
                            Math.round(supply * 100.0) / 100.0,
                            Math.round(supply * 0.3025 * 10.0) / 10.0);
                })
                .filter(r -> r.supplyAreaSqm() > 0)
                .distinct()
                .sorted(Comparator.comparingDouble(FloorTypeResponse::supplyAreaSqm))
                .toList();
    }

    private static final List<String> RESIDENTIAL_COMMON_KEYWORDS =
            List.of("계단", "승강기", "복도", "홀", "현관");

    private boolean isResidentialCommon(String etcPurps) {
        if (etcPurps == null || etcPurps.isBlank()) return false;
        return RESIDENTIAL_COMMON_KEYWORDS.stream().anyMatch(etcPurps::contains);
    }

    private HouseSearchResponse toHouseSearchResponse(Document doc) {
        String bCode = doc.address().bCode();           // 10자리 법정동코드
        String sigunguCd = bCode.substring(0, 5);
        String bjdongCd = bCode.substring(5, 10);

        String buildingName = doc.roadAddress() != null ? doc.roadAddress().buildingName() : "";
        String roadAddress = doc.roadAddress() != null ? doc.roadAddress().addressName() : doc.address().addressName();

        return new HouseSearchResponse(
                buildingName,
                roadAddress,
                sigunguCd,
                bjdongCd,
                doc.address().mainAddressNo(),
                doc.address().subAddressNo()
        );
    }
}
