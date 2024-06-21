package ru.dbhub;

import jakarta.validation.constraints.NotNull;

public record PublisherConfig(
    @NotNull Long rate,
    @NotNull Integer limit
) {
}
