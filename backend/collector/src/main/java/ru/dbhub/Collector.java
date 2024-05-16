package ru.dbhub;

import java.io.IOException;
import java.util.*;

class Collector {
    private final CollectorConfig config;

    private final ArticleStorage storage;

    Collector(CollectorConfig config, ArticleStorage storage) {
        this.config = config;
        this.storage = storage;
    }

    private boolean textContainsKeyword(String text) {
        return Arrays.stream(text.split("\\s+"))
            .map(String::toLowerCase)
            .anyMatch(config.keywords()::contains);
    }

    private boolean shouldCollectByTopic(JustCollectedArticle article) {
        return textContainsKeyword(article.title()) || textContainsKeyword(article.text());
    }

    void collect(String sourceName, NewsSource source, boolean requiresFiltering) throws IOException {
        long oldLastTimestamp = storage.getLastTimestampOfSource(sourceName);
        long newLastTimestamp = 0;

        List<JustCollectedArticle> articles = new ArrayList<>();

        while (true) {
            var articlesPage = source.nextArticlesPage();
            if (newLastTimestamp == 0 && !articlesPage.isEmpty()) {
                newLastTimestamp = articlesPage.getFirst().timestamp();
            }

            articlesPage.stream()
                .takeWhile(article -> article.timestamp() >= oldLastTimestamp)
                .filter(article -> !requiresFiltering || shouldCollectByTopic(article))
                .filter(article -> !storage.has(sourceName, article.link()))
                .forEach(articles::add);

            boolean shouldStopCollecting = articlesPage.isEmpty()
                || articlesPage.getLast().timestamp() < oldLastTimestamp;
            if (shouldStopCollecting) {
                break;
            }
        }

        articles.forEach(article -> storage.addJustCollected(sourceName, article));
        storage.setLastTimestampOfSource(sourceName, newLastTimestamp);
    }
}
