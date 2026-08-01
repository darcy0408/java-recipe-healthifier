package com.healthifier.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.healthifier.domain.ConvertInput;
import com.healthifier.domain.RuleCompliance;
import com.healthifier.domain.RuleId;
import com.healthifier.infrastructure.InMemorySwapRuleRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TextRecipeConversionServiceTest {
    private final ConversionService service =
        new TextRecipeConversionService(new InMemorySwapRuleRepository());

    @Test
    void convertsARecipeEndToEnd() {
        ConvertInput input = ConvertInput.builder()
            .text("""
                Low-Carb Cake
                Servings: 8

                Ingredients:
                - 2 cups all-purpose flour
                - 1 cup sugar
                - 1/2 cup vegetable oil
                - 2 eggs

                Instructions:
                1. Mix all ingredients.
                2. Bake until set.
                """)
            .appUserId("user-1")
            .build();

        var result = service.convert(new ConversionRequest(input,
            Set.of(RuleId.GLUTEN_FREE, RuleId.KETO, RuleId.NO_SEED_OILS)));

        assertEquals("Low-Carb Cake", result.title());
        assertEquals("8", result.servings());
        assertEquals(4, result.ingredients().size());
        assertEquals("2 cups almond flour", result.ingredients().get(0).text());
        assertEquals("1 cup allulose", result.ingredients().get(1).text());
        assertEquals("1/2 cup avocado oil", result.ingredients().get(2).text());
        assertFalse(result.ingredients().get(3).changed());
        assertEquals(3, result.swaps().size());
        assertEquals(2, result.steps().size());
        assertTrue(result.ruleCompliance().values().stream()
            .allMatch(value -> value == RuleCompliance.COMPLIANT));
    }

    @Test
    void reportsAnUnfixableCustomAvoidance() {
        ConvertInput input = new ConvertInput.Text("""
            Omelet
            Ingredients:
            - 2 eggs
            - black pepper
            Steps:
            - Cook everything.
            """, "user-1", false);

        var result = service.convert(new ConversionRequest(input, Set.of(RuleId.HIGH_PROTEIN),
            Optional.of("black pepper")));

        assertEquals(1, result.unfixable().size());
        assertEquals(RuleCompliance.NOT_POSSIBLE,
            result.ruleCompliance().get("CUSTOM_AVOID"));
    }

    @Test
    void rejectsMalformedRecipeText() {
        ConvertInput input = new ConvertInput.Text("Recipe without sections", "user-1", false);

        assertThrows(IllegalArgumentException.class,
            () -> service.convert(new ConversionRequest(input, Set.of(RuleId.KETO))));
    }

    @Test
    void rejectsInputsThatNeedAnIngestionAdapter() {
        ConvertInput input = new ConvertInput.Url("https://example.com/recipe", "user-1", false);

        assertThrows(UnsupportedOperationException.class,
            () -> service.convert(new ConversionRequest(input, Set.of(RuleId.KETO))));
    }
}
