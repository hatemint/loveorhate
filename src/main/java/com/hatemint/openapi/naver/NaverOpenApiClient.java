package com.hatemint.openapi.naver;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "naver-open-api",
        url = "${naver.openapi.url}",
        configuration = NaverOpenApiConfiguration.class
)
public interface NaverOpenApiClient {

    @GetMapping(value = "/v1/search/local.json")
    NaverSearchResponse searchPlaceByKeyword(@RequestParam("query") String keyword);
}
