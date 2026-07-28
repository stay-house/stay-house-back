package com.example.stay_house_back.client;

import com.example.stay_house_back.dto.kakao.KakaoAddressResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoAddressClient {

    private static final String BASE_URL = "https://dapi.kakao.com";

    private final RestClient restClient;

    public KakaoAddressClient(@Value("${kakao.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "KakaoAK " + apiKey)
                .build();
    }

    public KakaoAddressResponse searchAddress(String query) {
        return restClient.get()
                .uri("/v2/local/search/address.json?query={query}&size=10", query)
                .retrieve()
                .body(KakaoAddressResponse.class);
    }
}
