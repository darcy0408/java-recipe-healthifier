package com.healthifier.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record LibraryEntry(
    String id,
    Instant savedAt,
    ConversionResult result,
    List<RuleId> rules,
    String customAvoid,
    String sourceSummary,
    boolean isPaywalledConversion
) {
    public LibraryEntry {
        id = requireText(id, "id");
        savedAt = Objects.requireNonNull(savedAt, "savedAt");
        result = Objects.requireNonNull(result, "result");
        rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
        customAvoid = customAvoid == null ? "" : customAvoid.strip();
        sourceSummary = requireText(sourceSummary, "sourceSummary");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.strip();
    }
}
