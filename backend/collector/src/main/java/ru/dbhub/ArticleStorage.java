package ru.dbhub;

import java.util.List;
import java.util.Optional;

public interface ArticleStorage {
    List<Article> getAfter(long boundId);

    List<Article> getLast(int count);

    long getLastTimestampOfSource(String source);

    void setLastTimestampOfSource(String source, long timestamp);

    boolean has(String source, String link);

    void addJustCollected(String source, JustCollectedArticle justCollectedArticle);
}
