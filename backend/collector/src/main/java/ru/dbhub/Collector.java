package ru.dbhub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

class Collector {
    private final CollectorConfig config;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    Collector(CollectorConfig config) {
        this.config = config;
    }

    private boolean textContainsKeyword(String text) {
        return Arrays.stream(text.split("\\s+"))
            .map(String::toLowerCase)
            .anyMatch(config.keywords()::contains);
    }

    private boolean shouldCollectByTopic(JustCollectedArticle article) {
        return textContainsKeyword(article.title()) || textContainsKeyword(article.text());
    }

    void collect(
        String sourceName, NewsSource source, ArticleStorage storage
    ) throws IOException {
        List<JustCollectedArticle> articles = new ArrayList<>();
        long oldLastTimestamp = storage.getLastTimestampOfSource(sourceName);
        long newLastTimestamp = 0;

        for (int pageNo = 1;; ++pageNo) {
            logger.info("Started collecting page " + pageNo + " of source " + sourceName);

            var articlesPage = source.getArticlesPage(pageNo);
            if (newLastTimestamp == 0 && !articlesPage.isEmpty()) {
                newLastTimestamp = articlesPage.getFirst().timestamp();
            }

            logger.info("Found " + articlesPage.size() + " articles");

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
