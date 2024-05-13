package ru.dbhub;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.dbhub.mvc.CollectorConfigError;
import ru.dbhub.mvc.ErrorControllerAdvice;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/admin/collector-config")
public class CollectorConfigController {
    private final WebClient webClient;

    @Autowired
    private ErrorControllerAdvice errorControllerAdvice;

    CollectorConfigController(@Value("${ru.dbhub.collector-url}") String collectorUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(
                UriComponentsBuilder.fromUriString(collectorUrl)
                    .path("/configs")
                    .toUriString()
            )
            .build();
    }

    @GetMapping("/collector")
    public JsonNode getCollectorConfig() {
        return requireNonNull(
            webClient
                .get()
                .uri("/collector")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block()
        );
    }

    @PostMapping("/collector")
    public void setCollectorConfig(@RequestBody JsonNode config) {
        webClient
            .post()
            .uri("/collector")
            .bodyValue(config)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @GetMapping("/sources")
    public Map<String, NewsSourceTypeAndConfig> getNewsSourceConfigs() {
        return requireNonNull(
            webClient
                .get()
                .uri("/sources")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, NewsSourceTypeAndConfig>>() {})
                .block()
        );
    }

    @PostMapping("/sources")
    public void setNewsSourceConfigs(@RequestBody @Valid Map<String, @NotNull NewsSourceTypeAndConfig> sourceConfigs) {
        webClient
            .post()
            .uri("/sources")
            .bodyValue(sourceConfigs)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @PutMapping("/sources")
    public void resetNewsSourceConfigs(@RequestBody @Valid Map<String, @NotNull NewsSourceTypeAndConfig> sourceConfigs) {
        webClient
            .put()
            .uri("/sources")
            .bodyValue(sourceConfigs)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @DeleteMapping("/sources")
    public void deleteNewsSourceConfigs(@RequestBody @Valid List<@NotNull String> sourceNames) {
        webClient
            .method(HttpMethod.DELETE)
            .uri("/sources")
            .bodyValue(sourceNames)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @ExceptionHandler(WebClientResponseException.class)
    @Nullable
    private ResponseEntity<Object> handleWebClientResponseException(
        WebClientResponseException webClientResponseException, WebRequest request
    ) {
        if (webClientResponseException.getStatusCode().equals(BAD_REQUEST)) {
            var body = requireNonNull(webClientResponseException.getResponseBodyAs(ProblemDetail.class));
            if (CollectorConfigError.getByDetail(requireNonNull(body.getDetail())).isPresent()) {
                try {
                    return errorControllerAdvice.handleException(
                        new ResponseStatusException(BAD_REQUEST, body.getDetail()), request
                    );
                } catch (Exception unreachable) {
                    throw new RuntimeException(unreachable);
                }
            }
        }

        return errorControllerAdvice.handleAnyException(webClientResponseException, request);
    }
}
