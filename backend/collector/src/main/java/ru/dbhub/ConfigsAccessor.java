package ru.dbhub;

import java.util.Map;

public interface ConfigsAccessor {
    String getCollectorConfig();

    void setCollectorConfig(String collectorConfig);

    Map<String, NewsSourceTypeAndConfig> getNewsSourceConfigs();

    void setNewsSourceConfig(String source, NewsSourceTypeAndConfig typeAndConfig);
}
