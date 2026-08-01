package com.healthifier.domain;

public enum RuleId {
    NO_SEED_OILS,
    NO_UPF,
    KETO,
    LOW_CARB,
    CARNIVORE,
    GLUTEN_FREE,
    HIGH_PROTEIN,
    DAIRY_FREE;

    public SwapCategory category() {
        return SwapCategory.valueOf(name());
    }
}
