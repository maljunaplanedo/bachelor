package ru.dbhub;

import java.util.Optional;

public interface ArticlesAccessor {
    Optional<Article> getLastOfSource(String source);

    boolean has(String source, String link);

    void saveJustCollected(String source, JustCollectedArticle justCollectedArticle);
}
