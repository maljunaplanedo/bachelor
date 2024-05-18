package ru.dbhub;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

class Collector {
    private final CollectorConfig config;

    private final ArticleStorage storage;

    Collector(CollectorConfig config, ArticleStorage storage) {
        this.config = config;
        this.storage = storage;
    }

    private boolean textContainsKeyword(String text) {
        var lowerCaseText = text.toLowerCase();
        return config.keywords().stream()
            .anyMatch(
                keyword -> Pattern
                    .compile("(^|\\s)+%s($|[.!?\\s])+"
                    .formatted(keyword))
                    .matcher(lowerCaseText)
                    .find()
            );
    }

    private boolean shouldCollectByTopic(JustCollectedArticle article) {
        return textContainsKeyword(article.title()) || textContainsKeyword(article.text());
    }

    @Transactional(rollbackFor = {RuntimeException.class, IOException.class}, propagation = Propagation.NESTED)
    void collect(String sourceName, NewsSource source, boolean requiresFiltering) throws IOException {
        long oldLastTimestamp = storage.getLastTimestampOfSource(sourceName);
        long newLastTimestamp = 0;

        while (true) {
            var articlesPage = source.nextArticlesPage();
            if (newLastTimestamp == 0 && !articlesPage.isEmpty()) {
                newLastTimestamp = articlesPage.getFirst().timestamp();
            }

            articlesPage.stream()
                .takeWhile(article -> article.timestamp() >= oldLastTimestamp)
                .filter(article -> !requiresFiltering || shouldCollectByTopic(article))
                .filter(article -> !storage.has(article.link()))
                .forEach(article -> storage.addJustCollected(sourceName, article));

            boolean shouldStopCollecting = articlesPage.isEmpty()
                || articlesPage.getLast().timestamp() < oldLastTimestamp;
            if (shouldStopCollecting) {
                break;
            }
        }

        storage.setLastTimestampOfSource(sourceName, newLastTimestamp);
    }
}
