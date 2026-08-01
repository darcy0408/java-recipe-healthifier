package com.healthifier.application;

import com.healthifier.domain.ConvertInput;
import com.healthifier.domain.RuleId;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ConversionRequest(
    ConvertInput input,
    Set<RuleId> rules,
    Optional<String> customAvoid
) {
    public ConversionRequest {
        input = Objects.requireNonNull(input, "input");
        rules = Set.copyOf(Objects.requireNonNull(rules, "rules"));
        customAvoid = Objects.requireNonNull(customAvoid, "customAvoid")
            .map(String::strip)
            .filter(value -> !value.isEmpty());
        if (rules.isEmpty() && customAvoid.isEmpty()) {
            throw new IllegalArgumentException("At least one rule or custom avoidance is required");
        }
    }

    public ConversionRequest(ConvertInput input, Set<RuleId> rules) {
        this(input, rules, Optional.empty());
    }
}
