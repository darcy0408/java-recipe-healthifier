package com.healthifier.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.healthifier.application.TextRecipeConversionService;
import com.healthifier.infrastructure.InMemorySwapRuleRepository;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LocalRecipeServerTest {
    private static final Pattern CSRF = Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"");

    @Test
    void servesAccessibleFormAndConvertsRecipe() throws Exception {
        var service = new TextRecipeConversionService(new InMemorySwapRuleRepository());
        try (var server = new LocalRecipeServer(service, 0)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            URI home = URI.create("http://localhost:" + server.port() + "/");

            HttpResponse<String> form = client.send(HttpRequest.newBuilder(home).GET().build(),
                HttpResponse.BodyHandlers.ofString());

            assertEquals(200, form.statusCode());
            assertTrue(form.body().contains("<form action=\"/convert\" method=\"post\">"));
            assertTrue(form.body().contains("<fieldset><legend>Health goals</legend>"));
            assertTrue(form.body().contains("<label for=\"recipe\">"));
            assertTrue(form.headers().firstValue("Content-Security-Policy").orElseThrow()
                .contains("form-action 'self'"));
            String csrf = CSRF.matcher(form.body()).results().findFirst().orElseThrow().group(1);
            String recipe = """
                Cake
                Ingredients:
                - 1 cup all-purpose flour
                Instructions:
                - Mix the all-purpose flour.
                """;
            String body = field("csrf", csrf) + "&" + field("recipe", recipe)
                + "&" + field("rule", "GLUTEN_FREE");
            HttpRequest request = HttpRequest.newBuilder(home.resolve("/convert"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> result = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, result.statusCode());
            assertTrue(result.body().contains("1 cup almond flour"));
            assertTrue(result.body().contains("Mix the almond flour."));
            assertTrue(result.body().contains("COMPLIANT"));
            assertTrue(result.body().contains("not medical or nutritional certification"));
        }
    }

    @Test
    void rejectsInvalidCsrfAndEscapesSubmittedMarkup() throws Exception {
        var service = new TextRecipeConversionService(new InMemorySwapRuleRepository());
        try (var server = new LocalRecipeServer(service, 0)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            URI endpoint = URI.create("http://localhost:" + server.port() + "/convert");
            String recipe = """
                <script>alert('x')</script>
                Ingredients:
                - 1 egg
                Instructions:
                - Cook.
                """;
            String body = field("csrf", "wrong") + "&" + field("recipe", recipe)
                + "&" + field("rule", "HIGH_PROTEIN");
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(403, response.statusCode());
            assertFalse(response.body().contains("<script>alert"));
        }
    }

    @Test
    void rejectsOversizedForms() throws Exception {
        try (var server = new LocalRecipeServer(request -> null, 0)) {
            server.start();
            String body = "recipe=" + "x".repeat(100_100);
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("http://localhost:" + server.port() + "/convert"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString());

            assertEquals(413, response.statusCode());
        }
    }

    private static String field(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8) + "="
            + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
