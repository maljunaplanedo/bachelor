package ru.dbhub.mvc;

import java.util.Arrays;
import java.util.Optional;

public enum CollectorConfigError {
    BAD_FORMAT("Bad config format"),
    BAD_SOURCE_TYPE("Bad config source type");

    private final String detail;

    CollectorConfigError(String detail) {
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }

    public static Optional<CollectorConfigError> getByDetail(String detail) {
        return Arrays.stream(values())
            .filter(error -> error.getDetail().equals(detail))
            .findFirst();
    }

    @Override
    public String toString() {
        return getDetail();
    }
}
