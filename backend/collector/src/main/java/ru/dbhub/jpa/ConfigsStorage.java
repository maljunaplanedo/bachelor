package ru.dbhub.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private ObjectMapper jsonMapper;

    @Override
    public Optional<JsonNode> getCollectorConfig() {
        return collectorConfigRepository.findById(CollectorConfigModel.FAKE_ID)
            .map(CollectorConfigModel::getConfig)
            .map(config -> {
                try {
                    return jsonMapper.readTree(config);
                } catch (JsonProcessingException exception) {
                    throw new RuntimeException(exception);
                }
            });
    }

    @Override
    public void setCollectorConfig(JsonNode collectorConfig) {
        try {
            collectorConfigRepository.save(new CollectorConfigModel(jsonMapper.writeValueAsString(collectorConfig)));
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public Map<String, NewsSourceTypeAndConfig> getNewsSourceConfigs() {
        return newsSourceConfigRepository.findAll().stream()
            .collect(Collectors.toMap(
                NewsSourceConfigModel::getSource,
                newsSourceConfigModel -> {
                    try {
                        return new NewsSourceTypeAndConfig(
                            newsSourceConfigModel.getType(), jsonMapper.readTree(newsSourceConfigModel.getConfig())
                        );
                    } catch (JsonProcessingException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            ));
    }

    @Override
    public void setNewsSourceConfig(String source, NewsSourceTypeAndConfig typeAndConfig) {
        try {
            newsSourceConfigRepository.save(
                new NewsSourceConfigModel(
                    source, typeAndConfig.type(), jsonMapper.writeValueAsString(typeAndConfig.config())
                )
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeNewsSourceConfig(String source) {
        newsSourceConfigRepository.deleteById(source);
    }

    @Override
    public void removeAllNewsSourceConfigs() {
        newsSourceConfigRepository.deleteAll();
    }
}
