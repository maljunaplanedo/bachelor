package ru.dbhub;

public interface Storage<A> {
    StorageTransaction<A> getTransaction();
}
