package ru.dbhub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/articles")
public class ArticlesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

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
    public List<Article> getArticlesAfter(@RequestParam long boundId) {
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
                .bodyToMono(new ParameterizedTypeReference<List<Article>>() {})
                .block()
        );
    }

    @GetMapping("/last")
    public List<Article> getLastArticles(@RequestParam int count) {
        return Objects.requireNonNull(
            webClient
                .get()
                .uri(
                    uriBuilder -> uriBuilder
                        .path("/last")
                        .queryParam("count", count)
                        .build()
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Article>>() {})
                .block()
        );
    }
}
