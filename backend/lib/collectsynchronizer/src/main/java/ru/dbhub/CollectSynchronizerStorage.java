package ru.dbhub;

import java.util.Optional;

public interface CollectSynchronizerStorage {
    void addRecord(String groupName, long order, String id, long timestamp);

    Optional<String> getIdOfLeastByOrderAndIdAfterTimestamp(String groupName, long timestamp);
}
