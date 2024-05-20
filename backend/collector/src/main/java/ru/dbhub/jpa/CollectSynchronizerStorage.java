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
@Table(name = "CollectSync", indexes = {@Index(columnList = "recordId"), @Index(columnList = "timestamp")})
class CollectSynchronizerRecordModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    @Column(name = "\"order\"")
    private Long order;

    private String id;

    private Long timestamp;

    private CollectSynchronizerRecordModel() {
    }

    CollectSynchronizerRecordModel(Long order, String id, Long timestamp) {
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
        "WHERE timestamp > ?1 " +
        "ORDER BY order, id ASC " +
        "LIMIT 1"
    )
    Optional<CollectSynchronizerRecordModel> findLeastByOrderAndIdAfterTimestamp(long timestamp);
}

@Component
class CollectSynchronizerStorageImpl implements CollectSynchronizerStorage {
    @Autowired
    private CollectSynchronizerRepository collectSynchronizerRepository;

    @Override
    public void addRecord(long order, String id, long timestamp) {
        collectSynchronizerRepository.save(new CollectSynchronizerRecordModel(order, id, timestamp));
    }

    @Override
    public Optional<String> getIdOfLeastByOrderAndIdAfterTimestamp(long timestamp) {
        return collectSynchronizerRepository
            .findLeastByOrderAndIdAfterTimestamp(timestamp)
            .map(CollectSynchronizerRecordModel::getId);
    }
}
