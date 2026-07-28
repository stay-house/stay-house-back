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
            Double area                 // 전용면적 ㎡
    ) {}
}
