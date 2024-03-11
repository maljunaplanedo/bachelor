package ru.dbhub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class Collector {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final WebClient webClient;

    Collector(WebClient webClient) {
        this.webClient = webClient;
    }

    @Scheduled(fixedRateString = "${ru.dbhub.collector.collect-rate}")
    public void collect() {
        logger.debug(
            webClient
                .get()
                .uri("https://habr.com/ru/rss/articles/?fl=ru")
                .retrieve()
                .bodyToMono(String.class)
                .block()
        );
    }
}
