package ru.dbhub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class PublisherService {
    private static final long RETRY_FIND_CONFIGS_RATE = 60;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final WebClient collectorWebClient;

    private final WebClient telegramWebClient;

    private final DateTimeFormatter dateTimeFormatter;

    @Autowired
    @Lazy
    private PublisherService self;

    @Autowired
    private PublisherConfigStorage publisherConfigStorage;

    @Autowired
    private PublisherOffsetStorage publisherOffsetStorage;

    @Autowired
    private CollectSynchronizer collectSynchronizer;

    PublisherService(
        @Value("${ru.dbhub.collector-url}") String collectorUrl,
        @Value("${ru.dbhub.publisher.telegram-bot-token}") String telegramBotToken,
        @Value("${ru.dbhub.publisher.telegram-channel-username}") String telegramChannelUsername
    ) {
        this.collectorWebClient = WebClient.create(
            UriComponentsBuilder.fromUriString(collectorUrl)
                .path("/articles")
                .toUriString()
        );

        this.telegramWebClient = WebClient.create(
            UriComponentsBuilder.fromUriString(
                "https://api.telegram.org/bot" + telegramBotToken + "/sendMessage"
            )
                .queryParam("chat_id", telegramChannelUsername)
                .toUriString()
        );

        this.dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    }

    @Transactional
    public Optional<PublisherConfig> getPublisherConfig() {
        return publisherConfigStorage.getConfig();
    }

    public void setPublisherConfig(PublisherConfig config) {
        publisherConfigStorage.setConfig(config);
    }

    private void schedulePublish(long delay) {
        scheduler.schedule(
            () -> {
                try {
                    self.publish();
                } catch (Exception exception) {
                    logger.error("Publish threw an exception", exception);
                }
            },
            delay,
            TimeUnit.SECONDS
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishAsync() {
        schedulePublish(0);
    }

    private ArticlesAndBoundId getNewArticles(PublisherConfig config) {
        return Objects.requireNonNull(
            collectorWebClient
                .get()
                .uri(
                    uriBuilder -> uriBuilder
                        .path("/after")
                        .queryParam("boundId", publisherOffsetStorage.getOffset())
                        .queryParam("limit", config.limit())
                        .build()
                )
                .retrieve()
                .bodyToMono(ArticlesAndBoundId.class)
                .block()
        );
    }

    private void sendArticleToTelegram(Article article) {
        var text = article.text();
        text = text.substring(0, Math.min(text.length(), 1000));

        int dots = 0;
        for (; dots <= 3; ++dots) {
            if (text.charAt(text.length() - dots - 1) != '.') {
                break;
            }
        }
        text = text + ".".repeat(3 - dots);

        var textWithLinkAndTime = text + '\n'
            + article.link() + '\n'
            + dateTimeFormatter.format(
                Instant.ofEpochSecond(article.timestamp())
                    .atZone(ZoneId.of("Europe/Moscow"))
            );

        telegramWebClient
            .get()
            .uri(
                uriBuilder -> uriBuilder
                    .queryParam("text", textWithLinkAndTime)
                    .build()
            )
            .retrieve()
            .toBodilessEntity()
            .block();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Transactional
    public void publish() {
        var publisherConfigOptional = getPublisherConfig();
        if (publisherConfigOptional.isEmpty()) {
            schedulePublish(RETRY_FIND_CONFIGS_RATE);
            return;
        }

        var publisherConfig = publisherConfigOptional.get();

        schedulePublish(publisherConfig.rate());

        if (!collectSynchronizer.shouldCollect()) {
            logger.info("Not publishing news because the synchronizer decided so");
            return;
        }

        logger.info("Starting to publish news");

        var articlesAndOffset = getNewArticles(publisherConfig);
        articlesAndOffset.articles().forEach(this::sendArticleToTelegram);
        publisherOffsetStorage.setOffset(articlesAndOffset.boundId());

        logger.info("Finished publishing news");
    }
}
