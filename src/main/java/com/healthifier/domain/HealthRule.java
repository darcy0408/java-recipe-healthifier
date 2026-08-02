package com.healthifier.domain;

import java.util.Objects;
import java.util.Set;

public record HealthRule(
    RuleId id,
    Set<String> violationTerms,
    Set<String> evidenceTerms,
    boolean evidenceRequired
) {
    public HealthRule {
        id = Objects.requireNonNull(id, "id");
        violationTerms = normalized(violationTerms, "violationTerms");
        evidenceTerms = normalized(evidenceTerms, "evidenceTerms");
        if (evidenceRequired && evidenceTerms.isEmpty()) {
            throw new IllegalArgumentException("An evidence-based rule requires evidence terms");
        }
    }

    private static Set<String> normalized(Set<String> terms, String name) {
        Objects.requireNonNull(terms, name);
        return terms.stream().map(term -> Objects.requireNonNull(term, name + " element").strip())
            .filter(term -> !term.isEmpty()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
