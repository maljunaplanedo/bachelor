package ru.dbhub;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class Collector {
    public record Config(
        long maxArticles,
        Set<String> keywords
    ) {
    };

    private final Config config;

    Collector(Config config) {
        this.config = config;
    }

    private boolean textContainsKeyword(String text) {
        return Arrays.stream(text.split("\\s+")).anyMatch(config.keywords()::contains);
    }

    private boolean shouldCollect(JustCollectedArticle article) {
        return true;
        // return textContainsKeyword(article.title()) || textContainsKeyword(article.text());
    }

    public List<JustCollectedArticle> getArticlesFromSourceSince(
        NewsSource newsSource, int boundTimestamp
    ) throws IOException {
        List<JustCollectedArticle> articles = new ArrayList<>();

        for (int pageNo = 1;; ++pageNo) {
            var articlesPage = newsSource.getArticlesPage(pageNo);

            articlesPage.stream()
                .filter(this::shouldCollect)
                .limit(config.maxArticles() - articles.size())
                .takeWhile(article -> article.timestamp() >= boundTimestamp)
                .forEach(articles::add);

            if (articles.size() == config.maxArticles() ||
                articlesPage.isEmpty() ||
                articlesPage.getLast().timestamp() < boundTimestamp
            ) {
                break;
            }
        }

        return articles;
    }
}
