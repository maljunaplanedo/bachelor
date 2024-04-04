package ru.dbhub;

import java.util.List;
import java.util.Optional;

public interface ArticleStorage {
    List<Article> getAfter(long boundId);

    List<Article> getPage(long boundId, int page, int count);

    Optional<Long> getMaxId();

    long getLastTimestampOfSource(String source);

    void setLastTimestampOfSource(String source, long timestamp);

    boolean has(String source, String link);

    void addJustCollected(String source, JustCollectedArticle justCollectedArticle);
}
