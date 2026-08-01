package com.healthifier.domain;

public enum SwapCategory {
    NO_SEED_OILS,
    NO_UPF,
    KETO,
    LOW_CARB,
    CARNIVORE,
    GLUTEN_FREE,
    HIGH_PROTEIN,
    DAIRY_FREE,
    CUSTOM_AVOID,
    OTHER,
    UNKNOWN;

    public boolean isRuleCategory() {
        try {
            RuleId.valueOf(name());
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
