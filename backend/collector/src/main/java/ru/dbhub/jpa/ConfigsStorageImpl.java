package ru.dbhub.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.dbhub.ConfigsStorage;
import ru.dbhub.NewsSource;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ConfigsStorageImpl implements ConfigsStorage {
    @Autowired
    private CollectorConfigRepository collectorConfigRepository;

    @Autowired
    private NewsSourceConfigRepository newsSourceConfigRepository;

    @Override
    public String getCollectorConfig() {
        return collectorConfigRepository.findById(CollectorConfigModel.FAKE_ID).orElseThrow().getConfig();
    }

    @Override
    public void setCollectorConfig(String collectorConfig) {
        collectorConfigRepository.save(new CollectorConfigModel(collectorConfig));
    }

    @Override
    public Map<String, NewsSource.TypeAndConfig> getNewsSourceConfigs() {
        return newsSourceConfigRepository.findAll().stream()
            .collect(Collectors.toMap(
                NewsSourceConfigModel::getSource,
                newsSourceConfigModel -> new NewsSource.TypeAndConfig(
                    newsSourceConfigModel.getType(), newsSourceConfigModel.getConfig()
                )
            ));
    }

    @Override
    public void setNewsSourceConfig(String source, NewsSource.TypeAndConfig typeAndConfig) {
        newsSourceConfigRepository.save(
            new NewsSourceConfigModel(source, typeAndConfig.type(), typeAndConfig.config())
        );
    }
}
