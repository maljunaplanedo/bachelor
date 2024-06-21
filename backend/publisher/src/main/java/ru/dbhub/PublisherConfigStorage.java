package ru.dbhub;

import java.util.Optional;

public interface PublisherConfigStorage {
    Optional<PublisherConfig> getConfig();
    void setConfig(PublisherConfig config);
}
