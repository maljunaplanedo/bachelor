package ru.dbhub;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import ru.dbhub.mvc.ErrorControllerAdvice;

import java.util.Objects;

@RestController
public class FrontendController {
    private final WebClient webClient;

    @Autowired
    private ErrorControllerAdvice errorControllerAdvice;

    public FrontendController(@Value("${ru.dbhub.frontend-url}") String frontendUrl) {
        this.webClient = WebClient.create(frontendUrl);
    }

    @GetMapping("/**")
    public ResponseEntity<String> getStaticResource(HttpServletRequest request) {
        return Objects.requireNonNull(
            webClient
                .get()
                .uri(
                    uriBuilder -> uriBuilder
                        .path(request.getRequestURI())
                        .build()
                )
                .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
                .block()
        );
    }
}
