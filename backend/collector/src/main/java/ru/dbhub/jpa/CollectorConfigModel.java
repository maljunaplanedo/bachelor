package ru.dbhub.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "collector_config")
public class CollectorConfigModel {
    static final long FAKE_ID = 0;

    @Id
    private Long id = FAKE_ID;

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
