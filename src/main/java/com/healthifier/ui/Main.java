package com.healthifier.ui;

import com.healthifier.application.ConversionRequest;
import com.healthifier.application.ConversionService;
import com.healthifier.application.IngestingConversionService;
import com.healthifier.application.TextRecipeConversionService;
import com.healthifier.domain.ConversionResult;
import com.healthifier.domain.ConvertInput;
import com.healthifier.domain.ConvertedIngredient;
import com.healthifier.domain.LibraryEntry;
import com.healthifier.domain.RuleId;
import com.healthifier.infrastructure.FileRecipeLibrary;
import com.healthifier.infrastructure.InMemorySwapRuleRepository;
import com.healthifier.infrastructure.HttpRecipeSourceReader;
import com.healthifier.infrastructure.RecipeLibraryException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        ConversionService textService =
            new TextRecipeConversionService(new InMemorySwapRuleRepository());
        ConversionService service = new IngestingConversionService(
            textService, List.of(new HttpRecipeSourceReader()));
        int exitCode = run(args, service, System.out, System.err);
        if (exitCode != 0) System.exit(exitCode);
    }

    static int run(String[] args, ConversionService service, PrintStream out, PrintStream err) {
        try {
            CliOptions options = CliOptions.parse(args);
            if (options.help()) {
                out.println(usage());
                return 0;
            }
            FileRecipeLibrary library = new FileRecipeLibrary(options.libraryFile());
            switch (options.action()) {
                case SERVE -> serve(service, options.port(), out);
                case LIST -> printLibrary(library.findAll(), out);
                case SHOW -> {
                    LibraryEntry entry = library.findById(options.entryId())
                        .orElseThrow(() -> new IllegalArgumentException(
                            "No library entry found with id " + options.entryId()));
                    printResult(entry.result(), out);
                }
                case DELETE -> {
                    if (!library.deleteById(options.entryId())) {
                        throw new IllegalArgumentException(
                            "No library entry found with id " + options.entryId());
                    }
                    out.println("Deleted " + options.entryId());
                }
                case CONVERT -> {
                    ConvertInput input = options.toInput();
                    ConversionResult result = service.convert(
                        new ConversionRequest(input, options.rules(), options.customAvoid()));
                    printResult(result, out);
                    if (options.save()) {
                        LibraryEntry entry = options.toLibraryEntry(result);
                        library.save(entry);
                        out.println();
                        out.println("Saved as " + entry.id());
                    }
                }
            }
            return 0;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            err.println("Error: " + exception.getMessage());
            err.println(usage());
            return 2;
        } catch (IOException exception) {
            err.println("I/O error: " + exception.getMessage());
            return 3;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return 130;
        } catch (RecipeLibraryException exception) {
            err.println("Library error: " + exception.getMessage());
            return 4;
        }
    }

    private static void serve(ConversionService service, int port, PrintStream out)
            throws IOException, InterruptedException {
        try (LocalRecipeServer server = new LocalRecipeServer(service, port)) {
            server.start();
            out.println("Recipe Healthifier is available at:");
            server.accessUrls().forEach(url -> out.println("  " + url));
            out.println("Press Ctrl+C to stop.");
            server.awaitShutdown();
        }
    }

    private static void printLibrary(List<LibraryEntry> entries, PrintStream out) {
        if (entries.isEmpty()) {
            out.println("Recipe library is empty.");
            return;
        }
        out.println("Saved recipes:");
        entries.forEach(entry -> out.printf("- %s | %s | %s | %s%n", entry.id(),
            entry.savedAt(), entry.result().title(),
            entry.rules().stream().map(Enum::name).collect(Collectors.joining(","))));
    }

    private static void printResult(ConversionResult result, PrintStream out) {
        out.println(result.title());
        out.println("Servings: " + result.servings());
        out.println();
        out.println("Ingredients:");
        result.ingredients().forEach(ingredient -> printIngredient(ingredient, out));
        out.println();
        out.println("Instructions:");
        for (int index = 0; index < result.steps().size(); index++) {
            out.printf("%d. %s%n", index + 1, result.steps().get(index).text());
        }
        out.println();
        out.println("Compliance:");
        result.ruleCompliance().forEach((rule, status) ->
            out.printf("- %s: %s%n", rule, status));
        if (!result.unfixable().isEmpty()) {
            out.println();
            out.println("Needs attention:");
            result.unfixable().forEach(item -> out.println("- " + item));
        }
    }

    private static void printIngredient(ConvertedIngredient ingredient, PrintStream out) {
        out.print("- " + ingredient.text());
        if (!ingredient.appliedSwaps().isEmpty()) {
            out.print(" [swapped ");
            out.print(ingredient.appliedSwaps().stream()
                .map(swap -> swap.from() + " -> " + swap.to())
                .collect(Collectors.joining("; ")));
            out.print("]");
        }
        out.println();
    }

    private static String usage() {
        return """
            Usage:
              java ... com.healthifier.ui.Main (--file <recipe.txt> | --url <https://...>)
                  --rules <RULE[,RULE...]>
                  [--avoid <ingredient>] [--save]
                  [--library-file <path>]
              java ... com.healthifier.ui.Main --serve <port>
              java ... com.healthifier.ui.Main --library [--library-file <path>]
              java ... com.healthifier.ui.Main --show <entry-id> [--library-file <path>]
              java ... com.healthifier.ui.Main --delete <entry-id> [--library-file <path>]

            Rules:
              NO_SEED_OILS, NO_UPF, KETO, LOW_CARB, CARNIVORE, GLUTEN_FREE,
              HIGH_PROTEIN, DAIRY_FREE
            """.strip();
    }

    private record CliOptions(
        Path recipeFile,
        String recipeUrl,
        Set<RuleId> rules,
        Optional<String> customAvoid,
        boolean save,
        Action action,
        String entryId,
        int port,
        Path libraryFile,
        boolean help
    ) {
        private static CliOptions parse(String[] args) {
            if (args.length == 0 || Arrays.asList(args).contains("--help")) {
                return new CliOptions(null, null, Set.of(), Optional.empty(), false,
                    Action.CONVERT, null, 0, Path.of("recipe-library.json"), true);
            }

            Path file = null;
            String url = null;
            Set<RuleId> rules = Set.of();
            Optional<String> avoid = Optional.empty();
            boolean save = false;
            Action action = Action.CONVERT;
            String entryId = null;
            int port = 0;
            Path libraryFile = Path.of("recipe-library.json");
            int specialActions = 0;
            for (int index = 0; index < args.length; index++) {
                switch (args[index]) {
                    case "--file" -> file = Path.of(nextValue(args, ++index, "--file"));
                    case "--url" -> url = nextValue(args, ++index, "--url");
                    case "--rules" -> rules = parseRules(nextValue(args, ++index, "--rules"));
                    case "--avoid" -> avoid = Optional.of(nextValue(args, ++index, "--avoid"));
                    case "--save" -> save = true;
                    case "--serve" -> {
                        action = Action.SERVE;
                        port = parsePort(nextValue(args, ++index, "--serve"));
                        specialActions++;
                    }
                    case "--library" -> { action = Action.LIST; specialActions++; }
                    case "--show" -> {
                        action = Action.SHOW; entryId = nextValue(args, ++index, "--show"); specialActions++;
                    }
                    case "--delete" -> {
                        action = Action.DELETE; entryId = nextValue(args, ++index, "--delete"); specialActions++;
                    }
                    case "--library-file" -> libraryFile = Path.of(nextValue(args, ++index, "--library-file"));
                    default -> throw new IllegalArgumentException("Unknown option: " + args[index]);
                }
            }
            if (specialActions > 1) throw new IllegalArgumentException("Choose only one server or library command");
            if (action == Action.CONVERT) {
                if ((file == null) == (url == null)) {
                    throw new IllegalArgumentException("Provide exactly one of --file or --url");
                }
                if (rules.isEmpty() && avoid.isEmpty()) {
                    throw new IllegalArgumentException("--rules or --avoid is required");
                }
            } else if (file != null || url != null || save || !rules.isEmpty() || avoid.isPresent()) {
                throw new IllegalArgumentException("Conversion options cannot be combined with a server or library command");
            }
            return new CliOptions(file, url, rules, avoid, save, action, entryId, port,
                libraryFile, false);
        }

        private LibraryEntry toLibraryEntry(ConversionResult result) {
            String source = recipeUrl != null ? recipeUrl : recipeFile.toAbsolutePath().toString();
            return new LibraryEntry(UUID.randomUUID().toString(), Instant.now(), result,
                rules.stream().sorted().toList(), customAvoid.orElse(""), source, false);
        }

        private ConvertInput toInput() throws IOException {
            ConvertInput.Builder builder = ConvertInput.builder()
                .appUserId("local-cli")
                .preview(false);
            return recipeFile != null
                ? builder.text(Files.readString(recipeFile)).build()
                : builder.url(recipeUrl).build();
        }

        private static int parsePort(String value) {
            try {
                int port = Integer.parseInt(value);
                if (port < 1 || port > 65_535) throw new NumberFormatException();
                return port;
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("--serve port must be between 1 and 65535");
            }
        }

        private static Set<RuleId> parseRules(String value) {
            try {
                return Arrays.stream(value.split(","))
                    .map(String::strip)
                    .filter(token -> !token.isEmpty())
                    .map(token -> RuleId.valueOf(token.toUpperCase(Locale.ROOT)))
                    .collect(Collectors.toUnmodifiableSet());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown rule in: " + value, exception);
            }
        }

        private static String nextValue(String[] args, int index, String option) {
            if (index >= args.length || args[index].startsWith("--")) {
                throw new IllegalArgumentException(option + " requires a value");
            }
            return args[index];
        }
    }

    private enum Action { CONVERT, SERVE, LIST, SHOW, DELETE }
}
