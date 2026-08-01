package com.healthifier.domain;

public enum RuleCompliance {
    COMPLIANT,
    PARTIAL,
    NOT_POSSIBLE;

    public boolean isSatisfied() {
        return this == COMPLIANT;
    }
}
