package com.healthifier.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ConversionResult(
    String title,
    String servings,
    List<ConvertedIngredient> ingredients,
    List<ConvertedStep> steps,
    List<Swap> swaps,
    List<String> unfixable,
    Map<String, RuleCompliance> ruleCompliance
) {
    public ConversionResult {
        title = requireText(title, "title");
        servings = requireText(servings, "servings");
        ingredients = List.copyOf(Objects.requireNonNull(ingredients, "ingredients"));
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        swaps = List.copyOf(Objects.requireNonNull(swaps, "swaps"));
        unfixable = List.copyOf(Objects.requireNonNull(unfixable, "unfixable"));
        ruleCompliance = Map.copyOf(Objects.requireNonNull(ruleCompliance, "ruleCompliance"));
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String title;
        private String servings;
        private List<ConvertedIngredient> ingredients = List.of();
        private List<ConvertedStep> steps = List.of();
        private List<Swap> swaps = List.of();
        private List<String> unfixable = List.of();
        private Map<String, RuleCompliance> ruleCompliance = Map.of();

        private Builder() {}

        public Builder title(String value) { title = value; return this; }
        public Builder servings(String value) { servings = value; return this; }
        public Builder ingredients(List<ConvertedIngredient> value) { ingredients = value; return this; }
        public Builder steps(List<ConvertedStep> value) { steps = value; return this; }
        public Builder swaps(List<Swap> value) { swaps = value; return this; }
        public Builder unfixable(List<String> value) { unfixable = value; return this; }
        public Builder ruleCompliance(Map<String, RuleCompliance> value) { ruleCompliance = value; return this; }

        public ConversionResult build() {
            return new ConversionResult(title, servings, ingredients, steps, swaps, unfixable, ruleCompliance);
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.strip();
    }
}
