package com.hatemint.openapi.naver;

import com.hatemint.openapi.OpenApiSearchService;
import com.hatemint.openapi.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class NaverOpenAPiSearchService implements OpenApiSearchService {

    private final NaverOpenApiClient client;

    @Async
    @Override
    public CompletableFuture<SearchResponse> searchPlaceByKeyword(String keyword) {
        return CompletableFuture.completedFuture(client.searchPlaceByKeyword(keyword))
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        // log
                        return new NaverSearchResponse();
//                throw new RuntimeException(throwable);
                    }
                    return response;
                });
    }
}
