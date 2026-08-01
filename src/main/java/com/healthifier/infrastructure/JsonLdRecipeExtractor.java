package com.healthifier.infrastructure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JsonLdRecipeExtractor {
    private static final Pattern JSON_LD_SCRIPT = Pattern.compile(
        "(?is)<script\\b[^>]*type\\s*=\\s*(['\"])application/ld\\+json\\1[^>]*>(.*?)</script\\s*>");
    private static final Pattern TAG = Pattern.compile("(?is)<[^>]+>");

    private JsonLdRecipeExtractor() {}

    static String extract(String html) {
        Matcher scripts = JSON_LD_SCRIPT.matcher(html);
        while (scripts.find()) {
            try {
                Object root = new JsonParser(scripts.group(2)).parse();
                Optional<Map<String, Object>> recipe = findRecipe(root);
                if (recipe.isPresent()) return toRecipeText(recipe.orElseThrow());
            } catch (IllegalArgumentException ignored) {
                // A page can contain unrelated malformed JSON-LD; continue to the next block.
            }
        }
        throw new RecipeIngestionException("No Schema.org Recipe JSON-LD was found");
    }

    private static Optional<Map<String, Object>> findRecipe(Object value) {
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> object = stringMap(raw);
            if (isRecipeType(object.get("@type"))) return Optional.of(object);
            Object graph = object.get("@graph");
            if (graph != null) {
                Optional<Map<String, Object>> found = findRecipe(graph);
                if (found.isPresent()) return found;
            }
            for (Object child : object.values()) {
                Optional<Map<String, Object>> found = findRecipe(child);
                if (found.isPresent()) return found;
            }
        } else if (value instanceof List<?> values) {
            for (Object child : values) {
                Optional<Map<String, Object>> found = findRecipe(child);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
    }

    private static boolean isRecipeType(Object value) {
        if (value instanceof String text) return "Recipe".equalsIgnoreCase(text);
        if (value instanceof List<?> types) return types.stream().anyMatch(JsonLdRecipeExtractor::isRecipeType);
        return false;
    }

    private static String toRecipeText(Map<String, Object> recipe) {
        String name = requiredString(recipe, "name");
        List<String> ingredients = strings(recipe.get("recipeIngredient"));
        List<String> instructions = instructionStrings(recipe.get("recipeInstructions"));
        if (ingredients.isEmpty()) throw new RecipeIngestionException("Recipe has no ingredients");
        if (instructions.isEmpty()) throw new RecipeIngestionException("Recipe has no instructions");

        StringBuilder result = new StringBuilder(name).append('\n');
        optionalString(recipe.get("recipeYield"))
            .ifPresent(yield -> result.append("Servings: ").append(yield).append('\n'));
        result.append("Ingredients:\n");
        ingredients.forEach(item -> result.append("- ").append(cleanText(item)).append('\n'));
        result.append("Instructions:\n");
        for (int index = 0; index < instructions.size(); index++) {
            result.append(index + 1).append(". ").append(cleanText(instructions.get(index))).append('\n');
        }
        return result.toString();
    }

    private static List<String> instructionStrings(Object value) {
        List<String> result = new ArrayList<>();
        collectInstructions(value, result);
        return result;
    }

    private static void collectInstructions(Object value, List<String> result) {
        if (value instanceof String text) {
            if (!text.isBlank()) result.add(text);
        } else if (value instanceof List<?> values) {
            values.forEach(child -> collectInstructions(child, result));
        } else if (value instanceof Map<?, ?> raw) {
            Map<String, Object> object = stringMap(raw);
            Object text = object.get("text");
            if (text instanceof String instruction && !instruction.isBlank()) result.add(instruction);
            else if (object.containsKey("itemListElement")) {
                collectInstructions(object.get("itemListElement"), result);
            }
        }
    }

    private static List<String> strings(Object value) {
        if (value instanceof String text) return text.isBlank() ? List.of() : List.of(text);
        if (!(value instanceof List<?> values)) return List.of();
        return values.stream().filter(String.class::isInstance).map(String.class::cast)
            .filter(text -> !text.isBlank()).toList();
    }

    private static String requiredString(Map<String, Object> object, String key) {
        return optionalString(object.get(key))
            .orElseThrow(() -> new RecipeIngestionException("Recipe is missing " + key));
    }

    private static Optional<String> optionalString(Object value) {
        if (value instanceof String text && !text.isBlank()) return Optional.of(cleanText(text));
        if (value instanceof List<?> values) {
            return values.stream().filter(String.class::isInstance).map(String.class::cast)
                .filter(text -> !text.isBlank()).findFirst().map(JsonLdRecipeExtractor::cleanText);
        }
        return Optional.empty();
    }

    private static String cleanText(String value) {
        return decodeEntities(TAG.matcher(value).replaceAll(" ")).replaceAll("\\s+", " ").strip();
    }

    private static String decodeEntities(String value) {
        return value.replace("&amp;", "&").replace("&quot;", "\"")
            .replace("&#39;", "'").replace("&apos;", "'")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ");
    }

    private static Map<String, Object> stringMap(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, value) -> {
            if (key instanceof String text) result.put(text, value);
        });
        return result;
    }

    private static final class JsonParser {
        private final String source;
        private int index;

        private JsonParser(String source) { this.source = source; }

        private Object parse() {
            skipWhitespace();
            Object value = value();
            skipWhitespace();
            if (index != source.length()) fail("Unexpected trailing content");
            return value;
        }

        private Object value() {
            skipWhitespace();
            if (index >= source.length()) return fail("Unexpected end of JSON");
            return switch (source.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (take('}')) return result;
            do {
                skipWhitespace();
                if (source.charAt(index) != '"') fail("Object key must be a string");
                String key = string();
                skipWhitespace();
                expect(':');
                result.put(key, value());
                skipWhitespace();
            } while (take(','));
            expect('}');
            return result;
        }

        private List<Object> array() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (take(']')) return result;
            do {
                result.add(value());
                skipWhitespace();
            } while (take(','));
            expect(']');
            return result;
        }

        private String string() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < source.length()) {
                char character = source.charAt(index++);
                if (character == '"') return result.toString();
                if (character != '\\') {
                    result.append(character);
                    continue;
                }
                if (index >= source.length()) fail("Incomplete escape");
                char escaped = source.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(unicode());
                    default -> fail("Unknown escape");
                }
            }
            return fail("Unterminated string");
        }

        private char unicode() {
            if (index + 4 > source.length()) return fail("Incomplete Unicode escape");
            try {
                char result = (char) Integer.parseInt(source.substring(index, index + 4), 16);
                index += 4;
                return result;
            } catch (NumberFormatException exception) {
                return fail("Invalid Unicode escape");
            }
        }

        private Object number() {
            int start = index;
            while (index < source.length()
                    && "-+0123456789.eE".indexOf(source.charAt(index)) >= 0) index++;
            if (start == index) return fail("Expected JSON value");
            String token = source.substring(start, index);
            try {
                return token.contains(".") || token.contains("e") || token.contains("E")
                    ? Double.valueOf(token) : Long.valueOf(token);
            } catch (NumberFormatException exception) {
                return fail("Invalid number");
            }
        }

        private Object literal(String token, Object value) {
            if (!source.startsWith(token, index)) return fail("Invalid literal");
            index += token.length();
            return value;
        }

        private boolean take(char expected) {
            if (index < source.length() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (!take(expected)) fail("Expected '" + expected + "'");
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++;
        }

        private <T> T fail(String message) {
            throw new IllegalArgumentException(message + " at position " + index);
        }
    }
}
