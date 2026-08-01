package com.healthifier.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.healthifier.domain.ConvertInput;
import com.healthifier.domain.RuleId;
import com.healthifier.infrastructure.InMemorySwapRuleRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IngestingConversionServiceTest {

    @Test
    void normalizesUrlInputBeforeDelegating() {
        RecipeSourceReader reader = new RecipeSourceReader() {
            @Override public boolean supports(ConvertInput input) {
                return input instanceof ConvertInput.Url;
            }
            @Override public ConvertInput.Text read(ConvertInput input) {
                return new ConvertInput.Text("""
                    Cake
                    Ingredients:
                    - 1 cup sugar
                    Instructions:
                    - Bake.
                    """, input.appUserId(), input.preview());
            }
        };
        ConversionService service = new IngestingConversionService(
            new TextRecipeConversionService(new InMemorySwapRuleRepository()), List.of(reader));

        var result = service.convert(new ConversionRequest(
            new ConvertInput.Url("https://example.com/cake", "user-1", true),
            Set.of(RuleId.KETO)));

        assertEquals("1 cup allulose", result.ingredients().getFirst().text());
        assertTrue(result.ingredients().getFirst().changed());
    }

    @Test
    void rejectsSourcesWithoutAReader() {
        ConversionService service = new IngestingConversionService(request -> null, List.of());

        assertThrows(UnsupportedOperationException.class, () -> service.convert(
            new ConversionRequest(new ConvertInput.Image("YWJj", "image/png", "user-1", false),
                Set.of(RuleId.KETO))));
    }
}
