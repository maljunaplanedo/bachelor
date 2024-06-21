package ru.dbhub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
class CollectSynchronizer {
    private static final long HEARTBEAT_RATE = 60;

    private static final long INITIAL_SLEEP = 60;

    @Value("${ru.dbhub.collectsynchronizer.group}")
    private String groupName;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private CollectSynchronizerStorage collectSynchronizerStorage;

    private Long order;

    private String id;

    private void heartbeat() {
        collectSynchronizerStorage.addRecord(groupName, order, id, Instant.now().toEpochMilli());
        scheduler.schedule(this::heartbeat, HEARTBEAT_RATE, TimeUnit.SECONDS);
    }

    public boolean shouldCollect() {
        if (id == null) {
            order = Instant.now().toEpochMilli();
            id = UUID.randomUUID().toString();
            heartbeat();
            try {
                Thread.sleep(INITIAL_SLEEP * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return collectSynchronizerStorage
            .getIdOfLeastByOrderAndIdAfterTimestamp(groupName, Instant.now().toEpochMilli() - 2 * 1000 * HEARTBEAT_RATE)
            .orElseThrow()
            .equals(id);
    }
}
