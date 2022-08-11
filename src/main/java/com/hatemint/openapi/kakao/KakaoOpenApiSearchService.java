package com.hatemint.openapi.kakao;

import com.hatemint.openapi.OpenApiSearchService;
import com.hatemint.openapi.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class KakaoOpenApiSearchService implements OpenApiSearchService {

    private final KakaoOpenApiClient client;

    @Async
    @Override
    public CompletableFuture<SearchResponse> searchPlaceByKeyword(String keyword) {
        return CompletableFuture.completedFuture(client.findPlaceByKeyword(keyword));
    }
}
