package ru.dbhub;

public interface StorageTransaction<A> {
    A get();

    void commit();

    void rollback();
}
