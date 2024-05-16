package ru.dbhub;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;

public interface ConfigsStorage {
    Optional<JsonNode> getCollectorConfig();

    void setCollectorConfig(JsonNode collectorConfig);

    Map<String, NewsSourceConfig> getNewsSourceConfigs();

    void setNewsSourceConfig(String source, NewsSourceConfig typeAndConfig);

    void removeNewsSourceConfig(String source);

    void removeAllNewsSourceConfigs();
}
