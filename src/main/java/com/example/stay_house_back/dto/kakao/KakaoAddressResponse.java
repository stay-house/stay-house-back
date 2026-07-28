package com.example.stay_house_back.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KakaoAddressResponse(List<Document> documents) {

    public record Document(
            @JsonProperty("road_address") RoadAddress roadAddress,
            Address address
    ) {}

    public record RoadAddress(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("building_name") String buildingName,
            @JsonProperty("zone_no") String zoneNo
    ) {}

    public record Address(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("b_code") String bCode,            // 법정동코드 10자리
            @JsonProperty("main_address_no") String mainAddressNo,  // 번
            @JsonProperty("sub_address_no") String subAddressNo     // 지
    ) {}
}
