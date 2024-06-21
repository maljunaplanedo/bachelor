package ru.dbhub;

public interface PublisherOffsetStorage {
    long getOffset();
    void setOffset(long id);
}
