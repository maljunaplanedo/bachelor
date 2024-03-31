package ru.dbhub;

import java.util.Set;

record CollectorConfig(
    long rate,
    long maxArticlesPerSource,
    Set<String> keywords
) {
}
