package ru.dbhub.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import ru.dbhub.ConfigsAccessor;
import ru.dbhub.NewsSourceTypeAndConfig;
import ru.dbhub.Storage;

import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "collector_config")
class CollectorConfigModel {
    static final long FAKE_ID = 0;

    @Id
    private final Long id = FAKE_ID;

    @Lob
    private String config;

    public CollectorConfigModel() {
    }

    public CollectorConfigModel(String config) {
        this.config = config;
    }

    public String getConfig() {
        return config;
    }
}

@Repository
interface CollectorConfigRepository extends JpaRepository<CollectorConfigModel, Long> {
}

@Entity
@Table(name = "news_source_configs")
class NewsSourceConfigModel {
    @Id
    private String source;

    private String type;

    @Lob
    private String config;

    public NewsSourceConfigModel() {
    }

    public NewsSourceConfigModel(String source, String type, String config) {
        this.source = source;
        this.type = type;
        this.config = config;
    }

    public final String getSource() {
        return source;
    }

    public String getType() {
        return type;
    }

    public String getConfig() {
        return config;
    }
}

@Repository
interface NewsSourceConfigRepository extends JpaRepository<NewsSourceConfigModel, Long> {
}

class ConfigsAccessorImpl implements ConfigsAccessor {
    private final CollectorConfigRepository collectorConfigRepository;

    private final NewsSourceConfigRepository newsSourceConfigRepository;

    ConfigsAccessorImpl(
        CollectorConfigRepository collectorConfigRepository, NewsSourceConfigRepository newsSourceConfigRepository
    ) {
        this.collectorConfigRepository = collectorConfigRepository;
        this.newsSourceConfigRepository = newsSourceConfigRepository;
    }

    @Override
    public String getCollectorConfig() {
        return collectorConfigRepository.findById(CollectorConfigModel.FAKE_ID).orElseThrow().getConfig();
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
}

@Configuration
class ConfigsStorageConfig {
    @Bean
    Storage<ConfigsAccessor> configsStorage(
        CollectorConfigRepository collectorConfigRepository,
        NewsSourceConfigRepository newsSourceConfigRepository,
        PlatformTransactionManager platformTransactionManager
    ) {
        return new StorageImpl<>(
            ConfigsAccessor.class,
            new ConfigsAccessorImpl(collectorConfigRepository, newsSourceConfigRepository),
            platformTransactionManager
        );
    }
}
