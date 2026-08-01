package com.healthifier.domain;

import java.util.Objects;
import java.util.Optional;

public record ConvertedStep(
    String text,
    boolean changed,
    Optional<String> original,
    Optional<String> reason
) {
    public ConvertedStep {
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) throw new IllegalArgumentException("text must not be blank");
        text = text.strip();
        original = Objects.requireNonNull(original, "original");
        reason = Objects.requireNonNull(reason, "reason");
        if (!changed && original.isPresent()) {
            throw new IllegalArgumentException("An unchanged step cannot have an original value");
        }
    }

    public ConvertedStep(String text, boolean changed) {
        this(text, changed, Optional.empty(), Optional.empty());
    }
}
