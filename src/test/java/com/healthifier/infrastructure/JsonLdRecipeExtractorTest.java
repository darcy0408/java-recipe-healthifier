package com.healthifier.infrastructure;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JsonLdRecipeExtractorTest {

    @Test
    void extractsRecipeFromGraphWithStructuredInstructions() {
        String html = """
            <html><head>
            <script type="application/ld+json">
            {
              "@context": "https://schema.org",
              "@graph": [
                {"@type": "WebSite", "name": "Recipes"},
                {
                  "@type": ["Recipe", "NewsArticle"],
                  "name": "Tom &amp; Jo's Cake",
                  "recipeYield": ["8 servings"],
                  "recipeIngredient": ["1 cup sugar", "2 eggs"],
                  "recipeInstructions": [
                    {"@type": "HowToStep", "text": "<b>Mix</b> ingredients."},
                    {"@type": "HowToSection", "itemListElement": [
                      {"@type": "HowToStep", "text": "Bake for 20 minutes."}
                    ]}
                  ]
                }
              ]
            }
            </script></head></html>
            """;

        String recipe = JsonLdRecipeExtractor.extract(html);

        assertTrue(recipe.startsWith("Tom & Jo's Cake\nServings: 8 servings"));
        assertTrue(recipe.contains("- 1 cup sugar"));
        assertTrue(recipe.contains("1. Mix ingredients."));
        assertTrue(recipe.contains("2. Bake for 20 minutes."));
    }

    @Test
    void skipsMalformedJsonLdBeforeAValidRecipe() {
        String html = """
            <script type="application/ld+json">{broken}</script>
            <script type="application/ld+json">
              {"@type":"Recipe","name":"Eggs","recipeIngredient":["2 eggs"],
               "recipeInstructions":"Cook the eggs."}
            </script>
            """;

        assertTrue(JsonLdRecipeExtractor.extract(html).startsWith("Eggs\n"));
    }

    @Test
    void rejectsPagesWithoutARecipe() {
        assertThrows(RecipeIngestionException.class,
            () -> JsonLdRecipeExtractor.extract("<html><body>No recipe</body></html>"));
    }
}
