package com.healthifier.domain;

import java.util.Objects;
import java.util.Set;

public record SwapRule(
    String source,
    String replacement,
    String reason,
    String ratio,
    Set<SwapCategory> categories
) {
    public SwapRule {
        source = requireText(source, "source");
        replacement = requireText(replacement, "replacement");
        reason = requireText(reason, "reason");
        ratio = requireText(ratio, "ratio");
        categories = Set.copyOf(Objects.requireNonNull(categories, "categories"));
        if (categories.isEmpty()) throw new IllegalArgumentException("categories must not be empty");
    }

    public Swap toAppliedSwap(SwapCategory category) {
        if (!categories.contains(category)) throw new IllegalArgumentException("Category is not supported by this rule");
        return new Swap(source, replacement, reason, ratio, java.util.Optional.of(category));
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.strip();
    }
}
