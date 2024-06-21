package ru.dbhub.jpa;

import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.dbhub.PublisherOffsetStorage;

@Entity
@Table(name = "PublisherOffset")
class PublisherOffsetModel {
    static final long FAKE_ID = 0;

    @Id
    private final Long id = FAKE_ID;

    @Column(name = "\"offset\"")
    private Long offset;

    private PublisherOffsetModel() {
    }

    PublisherOffsetModel(long offset) {
        this.offset = offset;
    }

    long getOffset() {
        return offset;
    }
}

@Repository
interface PublisherOffsetRepository extends JpaRepository<PublisherOffsetModel, Long> {
}

@Component
class PublisherOffsetStorageImpl implements PublisherOffsetStorage {
    @Autowired
    private PublisherOffsetRepository publisherOffsetRepository;

    @Override
    public long getOffset() {
        return publisherOffsetRepository.findById(PublisherOffsetModel.FAKE_ID)
            .map(PublisherOffsetModel::getOffset)
            .orElse(0L);
    }

    @Override
    public void setOffset(long offset) {
        publisherOffsetRepository.save(new PublisherOffsetModel(offset));
    }
}
