package com.example.stay_house_back.client;

import com.example.stay_house_back.dto.building.BuildingRegistryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class BuildingRegistryClient {

    private static final String BASE_URL = "https://apis.data.go.kr/1613000/BldRgstHubService";

    private final RestClient restClient;
    private final String apiKey;

    public BuildingRegistryClient(@Value("${public-data.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder().build();
    }

    public BuildingRegistryResponse getExposedAreaInfo(
            String sigunguCd, String bjdongCd, String bun, String ji) {

        URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/getBrExposPubuseAreaInfo")
                .queryParam("serviceKey", apiKey)
                .queryParam("sigunguCd", sigunguCd)
                .queryParam("bjdongCd", bjdongCd)
                .queryParam("bun", String.format("%04d", Integer.parseInt(bun)))
                .queryParam("ji", String.format("%04d", Integer.parseInt(ji.isBlank() ? "0" : ji)))
                .queryParam("numOfRows", 100)
                .queryParam("pageNo", 1)
                .build(true)
                .toUri();

        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .body(BuildingRegistryResponse.class);
    }
}
