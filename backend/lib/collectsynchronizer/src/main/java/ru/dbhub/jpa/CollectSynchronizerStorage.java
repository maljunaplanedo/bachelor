package ru.dbhub.jpa;

import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.dbhub.CollectSynchronizerStorage;

import java.util.Optional;

@Entity
@Table(name = "CollectSyncV2", indexes = {@Index(columnList = "group_name,id"), @Index(columnList = "\"order\",id"), @Index(columnList = "timestamp")})
class CollectSynchronizerRecordModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    private String groupName;

    @Column(name = "\"order\"")
    private Long order;

    private String id;

    private Long timestamp;

    private CollectSynchronizerRecordModel() {
    }

    CollectSynchronizerRecordModel(String groupName, Long order, String id, Long timestamp) {
        this.groupName = groupName;
        this.order = order;
        this.id = id;
        this.timestamp = timestamp;
    }

    String getId() {
        return id;
    }
}

@Repository
interface CollectSynchronizerRepository extends JpaRepository<CollectSynchronizerRecordModel, Long> {
    @Query(
        "SELECT c " +
        "FROM CollectSynchronizerRecordModel c " +
        "WHERE groupName = ?1 " +
        "AND timestamp > ?2 " +
        "ORDER BY order, id ASC " +
        "LIMIT 1"
    )
    Optional<CollectSynchronizerRecordModel> findLeastByOrderAndIdAfterTimestamp(String groupName, long timestamp);
}

@Component
class CollectSynchronizerStorageImpl implements CollectSynchronizerStorage {
    @Autowired
    private CollectSynchronizerRepository collectSynchronizerRepository;

    @Override
    public void addRecord(String groupName, long order, String id, long timestamp) {
        collectSynchronizerRepository.save(new CollectSynchronizerRecordModel(groupName, order, id, timestamp));
    }

    @Override
    public Optional<String> getIdOfLeastByOrderAndIdAfterTimestamp(String groupName, long timestamp) {
        return collectSynchronizerRepository
            .findLeastByOrderAndIdAfterTimestamp(groupName, timestamp)
            .map(CollectSynchronizerRecordModel::getId);
    }
}
