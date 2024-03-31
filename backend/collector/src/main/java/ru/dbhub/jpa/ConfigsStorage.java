package ru.dbhub.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.dbhub.ConfigsStorage;
import ru.dbhub.NewsSourceTypeAndConfig;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Entity
@Table(name = "CollectorConfig")
class CollectorConfigModel {
    static final long FAKE_ID = 0;

    @Id
    private final Long id = FAKE_ID;

    @Column(columnDefinition = "TEXT")
    private String config;

    private CollectorConfigModel() {
    }

    CollectorConfigModel(String config) {
        this.config = config;
    }

    String getConfig() {
        return config;
    }
}

@Repository
interface CollectorConfigRepository extends JpaRepository<CollectorConfigModel, Long> {
}

@Entity
@Table(name = "NewsSourceConfigs")
class NewsSourceConfigModel {
    @Id
    private String source;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String config;

    private NewsSourceConfigModel() {
    }

    NewsSourceConfigModel(String source, String type, String config) {
        this.source = source;
        this.type = type;
        this.config = config;
    }

    String getSource() {
        return source;
    }

    String getType() {
        return type;
    }

    String getConfig() {
        return config;
    }
}

@Repository
interface NewsSourceConfigRepository extends JpaRepository<NewsSourceConfigModel, String> {
}

@Component
class ConfigsStorageImpl implements ConfigsStorage {
    @Autowired
    private CollectorConfigRepository collectorConfigRepository;

    @Autowired
    private NewsSourceConfigRepository newsSourceConfigRepository;

    @Override
    public Optional<String> getCollectorConfig() {
        return collectorConfigRepository.findById(CollectorConfigModel.FAKE_ID)
            .map(CollectorConfigModel::getConfig);
    }

    @Override
    public void setCollectorConfig(String collectorConfig) {
        collectorConfigRepository.save(new CollectorConfigModel(collectorConfig));
    }

    @Override
    public Map<String, NewsSourceTypeAndConfig> getNewsSourceConfigs() {
        return newsSourceConfigRepository.findAll().stream()
            .collect(Collectors.toMap(
                NewsSourceConfigModel::getSource,
                newsSourceConfigModel -> new NewsSourceTypeAndConfig(
                    newsSourceConfigModel.getType(), newsSourceConfigModel.getConfig()
                )
            ));
    }

    @Override
    public void setNewsSourceConfig(String source, NewsSourceTypeAndConfig typeAndConfig) {
        newsSourceConfigRepository.save(
            new NewsSourceConfigModel(source, typeAndConfig.type(), typeAndConfig.config())
        );
    }

    @Override
    public void removeNewsSourceConfig(String source) {
        newsSourceConfigRepository.deleteById(source);
    }
}
