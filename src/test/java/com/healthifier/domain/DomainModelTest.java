package com.healthifier.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DomainModelTest {

    @Test
    void buildsEachConversionInputVariant() {
        ConvertInput.Text text = assertInstanceOf(ConvertInput.Text.class,
            ConvertInput.builder().text(" 1 cup flour ").appUserId(" user-1 ").preview(true).build());
        assertEquals("1 cup flour", text.text());
        assertEquals("user-1", text.appUserId());
        assertTrue(text.preview());

        ConvertInput.Url url = assertInstanceOf(ConvertInput.Url.class,
            ConvertInput.builder().url("https://example.com/recipe").appUserId("user-1").build());
        assertEquals("https://example.com/recipe", url.url());

        ConvertInput.Image image = assertInstanceOf(ConvertInput.Image.class,
            ConvertInput.builder().image("YWJj", "image/png").appUserId("user-1").build());
        assertEquals("image/png", image.imageMediaType());
    }

    @Test
    void rejectsInvalidConversionInputs() {
        assertThrows(IllegalStateException.class,
            () -> ConvertInput.builder().appUserId("user-1").build());
        assertThrows(IllegalArgumentException.class,
            () -> new ConvertInput.Url("ftp://example.com/recipe", "user-1", false));
        assertThrows(IllegalArgumentException.class,
            () -> new ConvertInput.Image("YWJj", "text/plain", "user-1", false));
        assertThrows(NullPointerException.class,
            () -> new ConvertInput.Text("recipe", null, false));
    }

    @Test
    void modelsAConvertedIngredientWithQuantitiesAndSwap() {
        Swap swap = new Swap("sugar", "allulose", "Reduces added sugar", "1:1",
            Optional.of(SwapCategory.LOW_CARB));

        ConvertedIngredient ingredient = new ConvertedIngredient(
            "1 cup allulose", true, Optional.of("1 cup sugar"),
            Optional.of("Lower carbohydrate sweetener"), Optional.of("Use one-to-one"),
            Optional.of(BigDecimal.ONE), Optional.of(BigDecimal.ONE), Optional.of("cup"),
            Optional.of(swap));

        assertEquals(swap, ingredient.appliedSwap().orElseThrow());
        assertEquals(0, BigDecimal.ONE.compareTo(ingredient.convertedQuantity().orElseThrow()));
    }

    @Test
    void enforcesConvertedIngredientInvariants() {
        assertThrows(IllegalArgumentException.class,
            () -> new ConvertedIngredient("flour", true, Optional.empty(), Optional.empty(),
                Optional.empty()));
        assertThrows(IllegalArgumentException.class,
            () -> new ConvertedIngredient("flour", false, Optional.of("wheat flour"),
                Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class,
            () -> new ConvertedIngredient("flour", true, Optional.of("wheat flour"),
                Optional.empty(), Optional.empty(), Optional.of(BigDecimal.ONE), Optional.empty(),
                Optional.of("cup"), Optional.empty()));
    }

    @Test
    void conversionResultDefensivelyCopiesCollections() {
        List<ConvertedIngredient> ingredients = new ArrayList<>();
        ingredients.add(new ConvertedIngredient("1 egg", false, Optional.empty(), Optional.empty(),
            Optional.empty()));
        Map<String, RuleCompliance> compliance = new HashMap<>();
        compliance.put("HIGH_PROTEIN", RuleCompliance.COMPLIANT);

        ConversionResult result = ConversionResult.builder()
            .title("Eggs")
            .servings("1")
            .ingredients(ingredients)
            .steps(List.of(new ConvertedStep("Cook the egg", false)))
            .ruleCompliance(compliance)
            .build();

        ingredients.clear();
        compliance.clear();

        assertEquals(1, result.ingredients().size());
        assertEquals(RuleCompliance.COMPLIANT, result.ruleCompliance().get("HIGH_PROTEIN"));
        assertThrows(UnsupportedOperationException.class, () -> result.ingredients().clear());
        assertThrows(UnsupportedOperationException.class,
            () -> result.ruleCompliance().put("KETO", RuleCompliance.PARTIAL));
    }

    @Test
    void libraryEntryDefensivelyCopiesRules() {
        ConversionResult result = ConversionResult.builder()
            .title("Eggs")
            .servings("1")
            .build();
        List<RuleId> rules = new ArrayList<>(List.of(RuleId.HIGH_PROTEIN));

        LibraryEntry entry = new LibraryEntry("entry-1", Instant.EPOCH, result, rules,
            null, "Typed recipe", false);
        rules.clear();

        assertEquals(List.of(RuleId.HIGH_PROTEIN), entry.rules());
        assertEquals("", entry.customAvoid());
    }

    @Test
    void mapsRulesToSwapCategories() {
        assertEquals(SwapCategory.KETO, RuleId.KETO.category());
        assertTrue(SwapCategory.KETO.isRuleCategory());
        assertFalse(SwapCategory.CUSTOM_AVOID.isRuleCategory());
        assertTrue(RuleCompliance.COMPLIANT.isSatisfied());
        assertFalse(RuleCompliance.PARTIAL.isSatisfied());
    }
}
