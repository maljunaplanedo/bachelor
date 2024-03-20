package ru.dbhub;

import java.util.Map;
import java.util.Optional;

public interface ConfigsStorage {
    Optional<String> getCollectorConfig();

    void setCollectorConfig(String collectorConfig);

    Map<String, NewsSourceTypeAndConfig> getNewsSourceConfigs();

    void setNewsSourceConfig(String source, NewsSourceTypeAndConfig typeAndConfig);
}
