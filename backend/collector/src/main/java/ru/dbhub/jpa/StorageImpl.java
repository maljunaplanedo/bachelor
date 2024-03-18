package ru.dbhub.jpa;

import org.springframework.transaction.PlatformTransactionManager;
import ru.dbhub.Storage;
import ru.dbhub.StorageTransaction;

class StorageImpl<A> implements Storage<A> {
    private final Class<A> accessorInterface;

    private final A accessor;

    private final PlatformTransactionManager platformTransactionManager;

    StorageImpl(Class<A> accessorInterface, A accessor, PlatformTransactionManager platformTransactionManager) {
        this.accessorInterface = accessorInterface;
        this.accessor = accessor;
        this.platformTransactionManager = platformTransactionManager;
    }

    @Override
    public StorageTransaction<A> getTransaction() {
        return new StorageTransactionImpl<>(accessorInterface, accessor, platformTransactionManager);
    }
}
