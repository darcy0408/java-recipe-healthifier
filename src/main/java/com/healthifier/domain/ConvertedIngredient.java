package com.healthifier.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record ConvertedIngredient(
    String text,
    boolean changed,
    Optional<String> original,
    Optional<String> reason,
    Optional<String> ratioNote,
    Optional<BigDecimal> originalQuantity,
    Optional<BigDecimal> convertedQuantity,
    Optional<String> unit,
    java.util.List<Swap> appliedSwaps
) {
    public ConvertedIngredient {
        text = requireText(text, "text");
        original = nonNullOptional(original, "original");
        reason = nonNullOptional(reason, "reason");
        ratioNote = nonNullOptional(ratioNote, "ratioNote");
        originalQuantity = positiveOptional(originalQuantity, "originalQuantity");
        convertedQuantity = positiveOptional(convertedQuantity, "convertedQuantity");
        unit = nonNullOptional(unit, "unit").map(String::strip).filter(value -> !value.isEmpty());
        appliedSwaps = java.util.List.copyOf(Objects.requireNonNull(appliedSwaps, "appliedSwaps"));
        if (!changed && (original.isPresent() || !appliedSwaps.isEmpty())) {
            throw new IllegalArgumentException("An unchanged ingredient cannot have an original value or applied swap");
        }
        if (changed && original.isEmpty() && appliedSwaps.isEmpty()) {
            throw new IllegalArgumentException("A changed ingredient requires an original value or applied swap");
        }
        if (originalQuantity.isPresent() != convertedQuantity.isPresent()) {
            throw new IllegalArgumentException("originalQuantity and convertedQuantity must be provided together");
        }
    }

    public ConvertedIngredient(String text, boolean changed, Optional<String> original,
                               Optional<String> reason, Optional<String> ratioNote) {
        this(text, changed, original, reason, ratioNote, Optional.empty(), Optional.empty(),
            Optional.empty(), java.util.List.of());
    }

    public ConvertedIngredient(String text, boolean changed, Optional<String> original,
                               Optional<String> reason, Optional<String> ratioNote,
                               Optional<BigDecimal> originalQuantity,
                               Optional<BigDecimal> convertedQuantity, Optional<String> unit,
                               Optional<Swap> appliedSwap) {
        this(text, changed, original, reason, ratioNote, originalQuantity, convertedQuantity, unit,
            Objects.requireNonNull(appliedSwap, "appliedSwap").stream().toList());
    }

    public Optional<Swap> appliedSwap() {
        return appliedSwaps.stream().findFirst();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.strip();
    }

    private static <T> Optional<T> nonNullOptional(Optional<T> value, String name) {
        return Objects.requireNonNull(value, name);
    }

    private static Optional<BigDecimal> positiveOptional(Optional<BigDecimal> value, String name) {
        Objects.requireNonNull(value, name);
        value.ifPresent(quantity -> {
            if (quantity.signum() < 0) throw new IllegalArgumentException(name + " must not be negative");
        });
        return value;
    }
}
