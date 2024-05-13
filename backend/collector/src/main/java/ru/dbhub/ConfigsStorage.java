package ru.dbhub;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;

public interface ConfigsStorage {
    Optional<JsonNode> getCollectorConfig();

    void setCollectorConfig(JsonNode collectorConfig);

    Map<String, NewsSourceTypeAndConfig> getNewsSourceConfigs();

    void setNewsSourceConfig(String source, NewsSourceTypeAndConfig typeAndConfig);

    void removeNewsSourceConfig(String source);

    void removeAllNewsSourceConfigs();
}
