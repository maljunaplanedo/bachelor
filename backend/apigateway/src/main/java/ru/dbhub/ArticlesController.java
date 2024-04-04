package ru.dbhub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/articles")
public class ArticlesController {
    private final WebClient webClient;

    public ArticlesController(@Value("${ru.dbhub.collector-url}") String collectorUrl) {
        this.webClient = WebClient.create(
            UriComponentsBuilder.fromUriString(collectorUrl)
                .path("/articles")
                .toUriString()
        );
    }

    private String collectorUrl;

    @GetMapping("/after")
    public ArticlesAndBoundId getArticlesAfter(@RequestParam long boundId) {
        return Objects.requireNonNull(
            webClient
                .get()
                .uri(
                    uriBuilder -> uriBuilder
                        .path("/after")
                        .queryParam("boundId", boundId)
                        .build()
                )
                .retrieve()
                .bodyToMono(ArticlesAndBoundId.class)
                .block()
        );
    }

    @GetMapping("/page")
    public ArticlesAndBoundId getArticlesPage(
        @RequestParam long boundId, @RequestParam int page, @RequestParam int count
    ) {
        return Objects.requireNonNull(
            webClient
                .get()
                .uri(
                    uriBuilder -> uriBuilder
                        .path("/page")
                        .queryParam("boundId", boundId)
                        .queryParam("page", page)
                        .queryParam("count", count)
                        .build()
                )
                .retrieve()
                .bodyToMono(ArticlesAndBoundId.class)
                .block()
        );
    }
}
