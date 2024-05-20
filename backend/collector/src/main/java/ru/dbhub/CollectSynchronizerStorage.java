package ru.dbhub;

import java.util.Optional;

public interface CollectSynchronizerStorage {
    void addRecord(long order, String id, long timestamp);

    Optional<String> getIdOfLeastByOrderAndIdAfterTimestamp(long timestamp);
}
