package com.healthifier.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.healthifier.domain.ConversionResult;
import com.healthifier.domain.ConvertedIngredient;
import com.healthifier.domain.ConvertedStep;
import com.healthifier.domain.LibraryEntry;
import com.healthifier.domain.RuleCompliance;
import com.healthifier.domain.RuleId;
import com.healthifier.domain.Swap;
import com.healthifier.domain.SwapCategory;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileRecipeLibraryTest {
    @TempDir Path directory;

    @Test
    void roundTripsCompleteEntriesAndSortsNewestFirst() {
        Path file = directory.resolve("nested/library.json");
        FileRecipeLibrary library = new FileRecipeLibrary(file);
        LibraryEntry older = entry("old", Instant.parse("2026-01-01T00:00:00Z"));
        LibraryEntry newer = entry("new", Instant.parse("2026-02-01T00:00:00Z"));

        library.save(older);
        library.save(newer);

        assertEquals(List.of(newer, older), library.findAll());
        assertEquals(older, library.findById("old").orElseThrow());
        assertTrue(Files.exists(file));
        assertTrue(read(file).startsWith("["));
    }

    @Test
    void rejectsDuplicateIdsWithoutChangingTheStore() {
        FileRecipeLibrary library = new FileRecipeLibrary(directory.resolve("library.json"));
        LibraryEntry entry = entry("same", Instant.EPOCH);
        library.save(entry);

        assertThrows(RecipeLibraryException.class, () -> library.save(entry));
        assertEquals(List.of(entry), library.findAll());
    }

    @Test
    void deletesExistingEntriesAndReportsMissingIds() {
        FileRecipeLibrary library = new FileRecipeLibrary(directory.resolve("library.json"));
        library.save(entry("entry-1", Instant.EPOCH));

        assertTrue(library.deleteById("entry-1"));
        assertFalse(library.deleteById("entry-1"));
        assertTrue(library.findAll().isEmpty());
    }

    @Test
    void reportsCorruptJsonWithoutOverwritingIt() throws Exception {
        Path file = directory.resolve("library.json");
        Files.writeString(file, "{ definitely not valid json");
        FileRecipeLibrary library = new FileRecipeLibrary(file);

        assertThrows(RecipeLibraryException.class, library::findAll);
        assertEquals("{ definitely not valid json", Files.readString(file));
    }

    private static LibraryEntry entry(String id, Instant savedAt) {
        Swap swap = new Swap("sugar", "allulose", "Lower carbohydrate", "1:1",
            Optional.of(SwapCategory.KETO));
        ConvertedIngredient ingredient = new ConvertedIngredient("1 cup allulose", true,
            Optional.of("1 cup sugar"), Optional.of("Lower carbohydrate"), Optional.of("1:1"),
            Optional.of(BigDecimal.ONE), Optional.of(BigDecimal.ONE), Optional.of("cup"), Optional.of(swap));
        ConversionResult result = new ConversionResult("Cake", "8", List.of(ingredient),
            List.of(new ConvertedStep("Bake", false)), List.of(swap), List.of(),
            Map.of("KETO", RuleCompliance.COMPLIANT));
        return new LibraryEntry(id, savedAt, result, List.of(RuleId.KETO), "", "URL recipe", false);
    }

    private static String read(Path file) {
        try { return Files.readString(file); }
        catch (Exception exception) { throw new AssertionError(exception); }
    }
}
