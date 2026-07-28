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

import java.util.List;

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

        return response.body().items().item().stream()
                .filter(item -> "전유".equals(item.exposPubuseGbCdNm()))
                .map(item -> item.area())
                .filter(area -> area != null && area > 0)
                .distinct()
                .sorted()
                .map(area -> new FloorTypeResponse(area, Math.round(area * 0.3025 * 10.0) / 10.0))
                .toList();
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
