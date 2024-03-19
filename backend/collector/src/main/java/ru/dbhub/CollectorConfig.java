package ru.dbhub;

import java.util.Set;

record CollectorConfig(
    long rate,
    long maxArticles,
    Set<String> keywords
) {
}
