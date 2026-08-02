package com.healthifier.infrastructure;

import com.healthifier.application.RecipeSourceReader;
import com.healthifier.domain.ConvertInput;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

public final class HttpRecipeSourceReader implements RecipeSourceReader {
    private static final int MAX_RESPONSE_CHARACTERS = 5_000_000;
    private final HttpClient client;

    public HttpRecipeSourceReader() {
        this(HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_3)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build());
    }

    public HttpRecipeSourceReader(HttpClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public boolean supports(ConvertInput input) {
        return input instanceof ConvertInput.Url;
    }

    @Override
    public ConvertInput.Text read(ConvertInput input) {
        if (!(input instanceof ConvertInput.Url url)) {
            throw new IllegalArgumentException("HTTP reader requires a URL input");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url.url()))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "text/html,application/xhtml+xml")
            .header("User-Agent", "JavaRecipeHealthifier/1.0")
            .GET()
            .build();
        try {
            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RecipeIngestionException(
                    "Recipe page returned HTTP " + response.statusCode());
            }
            if (response.body().length() > MAX_RESPONSE_CHARACTERS) {
                throw new RecipeIngestionException("Recipe page exceeds the 5 MB safety limit");
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.isEmpty() && !contentType.toLowerCase().contains("text/html")
                    && !contentType.toLowerCase().contains("application/xhtml+xml")) {
                throw new RecipeIngestionException("URL did not return an HTML document");
            }
            String recipeText = JsonLdRecipeExtractor.extract(response.body());
            return new ConvertInput.Text(recipeText, url.appUserId(), url.preview());
        } catch (IOException exception) {
            throw new RecipeIngestionException("Unable to download recipe page", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RecipeIngestionException("Recipe download was interrupted", exception);
        }
    }
}
