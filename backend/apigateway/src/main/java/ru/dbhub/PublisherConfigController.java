package ru.dbhub;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/admin/publisher-config")
public class PublisherConfigController {
    private final WebClient webClient;

    public PublisherConfigController(@Value("${ru.dbhub.publisher-url}") String publisherUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(
                UriComponentsBuilder.fromUriString(publisherUrl)
                    .path("/config")
                    .toUriString()
            )
            .build();
    }

    @GetMapping("/config")
    public Optional<PublisherConfig> getPublisherConfig() {
        return requireNonNull(
            webClient
                .get()
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Optional<PublisherConfig>>() {})
                .block()
        );
    }

    @PostMapping("/config")
    public void setPublisherConfig(@RequestBody @Valid PublisherConfig config) {
        webClient
            .post()
            .bodyValue(config)
            .retrieve()
            .toBodilessEntity()
            .block();
    }
}
