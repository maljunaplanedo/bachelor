package ru.dbhub;

import java.util.List;

public record ArticlesAndBoundId(
    List<Article> articles,
    long boundId
) {
}
