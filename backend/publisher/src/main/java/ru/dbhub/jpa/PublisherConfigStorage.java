package ru.dbhub.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.dbhub.PublisherConfig;
import ru.dbhub.PublisherConfigStorage;

import java.util.Optional;

@Entity
@Table(name = "PublisherConfig")
class PublisherConfigModel {
    static final long FAKE_ID = 0;

    @Id
    private final Long id = FAKE_ID;

    @Column(columnDefinition = "TEXT")
    private String config;

    private PublisherConfigModel() {
    }

    PublisherConfigModel(String config) {
        this.config = config;
    }

    String getConfig() {
        return config;
    }
}

@Repository
interface PublisherConfigRepository extends JpaRepository<PublisherConfigModel, Long> {
}

@Component
class PublisherConfigStorageImpl implements PublisherConfigStorage {
    @Autowired
    private PublisherConfigRepository publisherConfigRepository;

    @Autowired
    private ObjectMapper jsonMapper;

    @Override
    public Optional<PublisherConfig> getConfig() {
        return publisherConfigRepository.findById(PublisherConfigModel.FAKE_ID)
            .map(PublisherConfigModel::getConfig)
            .map(config -> {
                try {
                    return jsonMapper.readValue(config, PublisherConfig.class);
                } catch (JsonProcessingException exception) {
                    throw new RuntimeException(exception);
                }
            });
    }

    @Override
    public void setConfig(PublisherConfig publisherConfig) {
        try {
            publisherConfigRepository.save(new PublisherConfigModel(jsonMapper.writeValueAsString(publisherConfig)));
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }
}
