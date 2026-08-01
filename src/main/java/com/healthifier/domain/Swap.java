package com.healthifier.domain;

import java.util.Optional;
import java.util.Objects;

public record Swap(
    String from,
    String to,
    String why,
    String ratio,
    Optional<SwapCategory> category
) {
    public Swap {
        from = requireText(from, "from");
        to = requireText(to, "to");
        why = requireText(why, "why");
        ratio = requireText(ratio, "ratio");
        category = Objects.requireNonNull(category, "category");
    }

    public Swap(String from, String to, String why, String ratio) {
        this(from, to, why, ratio, Optional.empty());
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.strip();
    }
}
