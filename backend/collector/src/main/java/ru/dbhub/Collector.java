package ru.dbhub;

import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

class Collector {
    private final CollectorConfig config;

    Collector(CollectorConfig config) {
        this.config = config;
    }

    private boolean textContainsKeyword(String text) {
        return Arrays.stream(text.split("\\s+")).anyMatch(config.keywords()::contains);
    }

    private boolean shouldCollectByTopic(JustCollectedArticle article) {
        return true;
        // return textContainsKeyword(article.title()) || textContainsKeyword(article.text());
    }

    void collect(
        String sourceName, NewsSource source, ArticleStorage storage
    ) throws IOException {
        List<JustCollectedArticle> articles = new ArrayList<>();
        long oldLastTimestamp = storage.getLastTimestampOfSource(sourceName);
        long newLastTimestamp = 0;

        for (int pageNo = 1;; ++pageNo) {
            var articlesPage = source.getArticlesPage(pageNo);
            if (newLastTimestamp == 0 && !articlesPage.isEmpty()) {
                newLastTimestamp = articlesPage.getFirst().timestamp();
            }

            articlesPage.stream()
                .takeWhile(article -> article.timestamp() >= oldLastTimestamp)
                .filter(this::shouldCollectByTopic)
                .filter(article -> !storage.has(sourceName, article.link()))
                .limit(config.maxArticles() - articles.size())
                .forEach(articles::add);

            boolean shouldStopCollecting = articles.size() == config.maxArticles()
                || articlesPage.isEmpty()
                || articlesPage.getLast().timestamp() < oldLastTimestamp;
            if (shouldStopCollecting) {
                break;
            }
        }

        storage.setLastTimestampOfSource(sourceName, newLastTimestamp);
        articles.reversed().forEach(article -> storage.addJustCollected(sourceName, article));
    }
}
