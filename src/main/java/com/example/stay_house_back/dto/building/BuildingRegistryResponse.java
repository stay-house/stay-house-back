package com.example.stay_house_back.dto.building;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

@JacksonXmlRootElement(localName = "response")
public record BuildingRegistryResponse(Header header, Body body) {

    public record Header(String resultCode, String resultMsg) {}

    public record Body(Items items) {}

    public record Items(
            @JacksonXmlElementWrapper(useWrapping = false)
            List<Item> item
    ) {}

    public record Item(
            String bldNm,               // 건물명
            String dongNm,              // 동명
            String hoNm,                // 호명
            String exposPubuseGbCdNm,   // 전유/공용 구분명
            String mainAtchGbCdNm,      // 주부속구분명 ("주건축물" = 본 건물, "부속건축물" = 부속)
            String mainPurpsCdNm,       // 주용도명 (주거용, 업무용, 주차장 등)
            String etcPurps,            // 기타용도
            Double area                 // 면적 ㎡
    ) {}
}
