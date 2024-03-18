package ru.dbhub;

public record JustCollectedArticle(
    String link,
    String title,
    String text,
    long timestamp
) {
}
