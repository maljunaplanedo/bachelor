package ru.dbhub;

import jakarta.validation.constraints.NotNull;

public record NewsSourceTypeAndConfig(
    @NotNull String type,
    @NotNull String config
) {
}
