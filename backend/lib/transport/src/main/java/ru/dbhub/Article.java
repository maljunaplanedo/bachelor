package ru.dbhub;

public record Article(
    long id,
    String source,
    String link,
    String title,
    String text,
    long timestamp
) {
}
