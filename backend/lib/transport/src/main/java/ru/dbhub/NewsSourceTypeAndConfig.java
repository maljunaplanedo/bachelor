package ru.dbhub;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record NewsSourceTypeAndConfig(
    @NotNull String type,
    @NotNull JsonNode config
) {
}
