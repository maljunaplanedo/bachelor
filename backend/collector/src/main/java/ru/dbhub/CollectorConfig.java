package ru.dbhub;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

record CollectorConfig(
    @NotNull Long rate,
    @NotNull Long maxArticlesPerSource,
    @NotNull Set<@NotNull String> keywords
) {
}
