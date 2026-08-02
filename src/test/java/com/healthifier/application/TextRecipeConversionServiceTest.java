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

    @Test
    void appliesLongestMatchesAndMultipleSwapsWithoutProducingCompoundNonsense() {
        ConvertInput input = new ConvertInput.Text("""
            Coated Chicken
            Ingredients:
            - 1 cup panko breadcrumbs and 2 tbsp brown sugar
            - 1 tbsp vegetable oil and 1 cup all-purpose flour
            Instructions:
            - Mix the brown sugar with the vegetable oil.
            - Dredge in all-purpose flour and panko breadcrumbs.
            """, "user-1", false);

        var result = service.convert(new ConversionRequest(input,
            Set.of(RuleId.KETO, RuleId.GLUTEN_FREE, RuleId.NO_SEED_OILS)));

        assertEquals("1 cup crushed pork rinds and 2 tbsp allulose",
            result.ingredients().get(0).text());
        assertEquals(2, result.ingredients().get(0).appliedSwaps().size());
        assertEquals("1 tbsp avocado oil and 1 cup almond flour",
            result.ingredients().get(1).text());
        assertEquals(2, result.ingredients().get(1).appliedSwaps().size());
        assertFalse(result.ingredients().get(0).text().contains("panko crushed"));
        assertFalse(result.ingredients().get(0).text().contains("brown allulose"));
        assertEquals("Mix the allulose with the avocado oil.", result.steps().get(0).text());
        assertEquals("Dredge in almond flour and crushed pork rinds.", result.steps().get(1).text());
        assertTrue(result.steps().stream().allMatch(step -> step.changed()));
    }

    @Test
    void reportsHonestPartialAndNotPossibleComplianceForUnknownViolations() {
        ConvertInput input = new ConvertInput.Text("""
            Chicken Parmesan
            Ingredients:
            - 2 cups whole wheat flour
            - 3 tbsp rice bran oil
            - 1 tbsp butter
            - 1/2 cup mozzarella cheese
            - 2 chicken breasts
            Instructions:
            - Coat and cook the chicken.
            """, "user-1", false);

        var result = service.convert(new ConversionRequest(input,
            Set.of(RuleId.GLUTEN_FREE, RuleId.NO_SEED_OILS, RuleId.DAIRY_FREE,
                RuleId.HIGH_PROTEIN)));

        assertEquals("2 cups almond flour", result.ingredients().get(0).text());
        assertEquals(RuleCompliance.COMPLIANT, result.ruleCompliance().get("GLUTEN_FREE"));
        assertEquals(RuleCompliance.NOT_POSSIBLE, result.ruleCompliance().get("NO_SEED_OILS"));
        assertEquals(RuleCompliance.PARTIAL, result.ruleCompliance().get("DAIRY_FREE"));
        assertEquals(RuleCompliance.COMPLIANT, result.ruleCompliance().get("HIGH_PROTEIN"));
        assertTrue(result.unfixable().stream().anyMatch(item -> item.contains("rice bran oil")));
        assertTrue(result.unfixable().stream().anyMatch(item -> item.contains("mozzarella cheese")));
    }

    @Test
    void evaluatesCustomAvoidEvenWhenAnotherSwapOccursOnTheSameLine() {
        ConvertInput input = new ConvertInput.Text("""
            Dressing
            Ingredients:
            - 1 tbsp vegetable oil and 1 tsp black pepper
            Instructions:
            - Mix.
            """, "user-1", false);

        var result = service.convert(new ConversionRequest(input, Set.of(RuleId.NO_SEED_OILS),
            Optional.of("black pepper")));

        assertEquals("1 tbsp avocado oil and 1 tsp black pepper",
            result.ingredients().getFirst().text());
        assertEquals(RuleCompliance.NOT_POSSIBLE,
            result.ruleCompliance().get("CUSTOM_AVOID"));
        assertTrue(result.unfixable().stream().anyMatch(item -> item.contains("black pepper")));
    }

    @Test
    void marksEvidenceBasedGoalPartialWhenNoProteinSourceIsRecognized() {
        ConvertInput input = new ConvertInput.Text("""
            Fruit Plate
            Ingredients:
            - 1 sliced apple
            Instructions:
            - Serve.
            """, "user-1", false);

        var result = service.convert(new ConversionRequest(input, Set.of(RuleId.HIGH_PROTEIN)));

        assertEquals(RuleCompliance.PARTIAL, result.ruleCompliance().get("HIGH_PROTEIN"));
    }

    @Test
    void choosesAReplacementCompatibleWithAllSelectedGoals() {
        ConvertInput input = new ConvertInput.Text("""
            Dinner
            Ingredients:
            - 8 oz spaghetti
            - 1 cup whole milk
            Instructions:
            - Add the whole milk to the spaghetti.
            """, "user-1", false);

        var result = service.convert(new ConversionRequest(input,
            Set.of(RuleId.GLUTEN_FREE, RuleId.KETO, RuleId.DAIRY_FREE)));

        assertEquals("8 oz zucchini noodles", result.ingredients().get(0).text());
        assertEquals("1 cup unsweetened almond beverage", result.ingredients().get(1).text());
        assertTrue(result.ruleCompliance().values().stream()
            .allMatch(status -> status == RuleCompliance.COMPLIANT));
        assertEquals("Add the unsweetened almond beverage to the zucchini noodles.",
            result.steps().getFirst().text());
    }
}
