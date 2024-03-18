package ru.dbhub;

import java.util.Map;

public interface ConfigsStorage {
    String getCollectorConfig();

    void setCollectorConfig(String collectorConfig);

    Map<String, NewsSource.TypeAndConfig> getNewsSourceConfigs();

    void setNewsSourceConfig(String source, NewsSource.TypeAndConfig typeAndConfig);
}
