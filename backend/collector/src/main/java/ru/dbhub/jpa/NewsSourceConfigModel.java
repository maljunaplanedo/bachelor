package ru.dbhub.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "news_source_configs")
public class NewsSourceConfigModel {
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

    public String getSource() {
        return source;
    }

    public String getType() {
        return type;
    }

    public String getConfig() {
        return config;
    }
}
