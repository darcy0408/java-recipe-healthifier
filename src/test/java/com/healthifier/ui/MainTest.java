package com.healthifier.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.healthifier.application.TextRecipeConversionService;
import com.healthifier.domain.ConversionResult;
import com.healthifier.domain.ConvertInput;
import com.healthifier.infrastructure.InMemorySwapRuleRepository;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void convertsARecipeFromTheCommandLine() throws Exception {
        Path recipe = temporaryDirectory.resolve("cake.txt");
        Files.writeString(recipe, """
            Simple Cake
            Servings: 4
            Ingredients:
            - 1 cup sugar
            - 2 eggs
            Instructions:
            1. Mix.
            2. Bake.
            """);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = Main.run(
            new String[] {"--file", recipe.toString(), "--rules", "KETO"},
            new TextRecipeConversionService(new InMemorySwapRuleRepository()),
            new PrintStream(stdout, true, StandardCharsets.UTF_8),
            new PrintStream(stderr, true, StandardCharsets.UTF_8));

        String output = stdout.toString(StandardCharsets.UTF_8);
        assertEquals(0, exitCode);
        assertTrue(output.contains("- 1 cup allulose [swapped sugar -> allulose]"));
        assertTrue(output.contains("- KETO: COMPLIANT"));
        assertEquals("", stderr.toString(StandardCharsets.UTF_8));
    }

    @Test
    void printsHelpWhenNoArgumentsAreProvided() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        int exitCode = Main.run(new String[0], request -> null,
            new PrintStream(stdout), new PrintStream(new ByteArrayOutputStream()));

        assertEquals(0, exitCode);
        assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("Usage:"));
    }

    @Test
    void reportsInvalidOptionsWithoutRunningConversion() {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = Main.run(new String[] {"--unknown"}, request -> null,
            new PrintStream(new ByteArrayOutputStream()), new PrintStream(stderr));

        assertEquals(2, exitCode);
        assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("Unknown option"));
    }

    @Test
    void routesUrlInputToTheConversionService() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        int exitCode = Main.run(
            new String[] {"--url", "https://example.com/recipe", "--rules", "KETO"},
            request -> {
                assertTrue(request.input() instanceof ConvertInput.Url);
                return new ConversionResult("Recipe", "1", java.util.List.of(),
                    java.util.List.of(), java.util.List.of(), java.util.List.of(), Map.of());
            },
            new PrintStream(stdout), new PrintStream(new ByteArrayOutputStream()));

        assertEquals(0, exitCode);
        assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("Recipe"));
    }

    @Test
    void savesListsShowsAndDeletesAConvertedRecipe() throws Exception {
        Path recipe = temporaryDirectory.resolve("cake.txt");
        Path library = temporaryDirectory.resolve("library.json");
        Files.writeString(recipe, """
            Saved Cake
            Ingredients:
            - 1 cup sugar
            Instructions:
            - Bake.
            """);
        var service = new TextRecipeConversionService(new InMemorySwapRuleRepository());

        CommandResult saved = command(service, "--file", recipe.toString(), "--rules", "KETO",
            "--save", "--library-file", library.toString());
        assertEquals(0, saved.exitCode());
        String id = saved.stdout().lines().filter(line -> line.startsWith("Saved as "))
            .findFirst().orElseThrow().substring("Saved as ".length());

        CommandResult listed = command(service, "--library", "--library-file", library.toString());
        assertEquals(0, listed.exitCode());
        assertTrue(listed.stdout().contains(id));
        assertTrue(listed.stdout().contains("Saved Cake"));

        CommandResult shown = command(service, "--show", id, "--library-file", library.toString());
        assertEquals(0, shown.exitCode());
        assertTrue(shown.stdout().contains("1 cup allulose"));

        CommandResult deleted = command(service, "--delete", id, "--library-file", library.toString());
        assertEquals(0, deleted.exitCode());
        assertTrue(deleted.stdout().contains("Deleted " + id));

        CommandResult empty = command(service, "--library", "--library-file", library.toString());
        assertTrue(empty.stdout().contains("library is empty"));
    }

    @Test
    void rejectsCombiningLibraryAndConversionCommands() {
        CommandResult result = command(request -> null, "--library", "--url",
            "https://example.com", "--rules", "KETO");

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("cannot be combined"));
    }

    @Test
    void rejectsInvalidServerPortWithoutStartingServer() {
        CommandResult result = command(request -> null, "--serve", "70000");

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("port must be between 1 and 65535"));
    }

    private static CommandResult command(com.healthifier.application.ConversionService service,
                                         String... arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = Main.run(arguments, service, new PrintStream(stdout), new PrintStream(stderr));
        return new CommandResult(exitCode, stdout.toString(StandardCharsets.UTF_8),
            stderr.toString(StandardCharsets.UTF_8));
    }

    private record CommandResult(int exitCode, String stdout, String stderr) {}
}
