package com.healthifier.infrastructure;

import com.healthifier.domain.ConversionResult;
import com.healthifier.domain.ConvertedIngredient;
import com.healthifier.domain.ConvertedStep;
import com.healthifier.domain.LibraryEntry;
import com.healthifier.domain.RuleCompliance;
import com.healthifier.domain.RuleId;
import com.healthifier.domain.Swap;
import com.healthifier.domain.SwapCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class LibraryEntryJsonCodec {
    private LibraryEntryJsonCodec() {}

    static String encode(List<LibraryEntry> entries) {
        return JsonDocument.write(entries.stream().map(LibraryEntryJsonCodec::entryToMap).toList());
    }

    static List<LibraryEntry> decode(String json) {
        Object root = JsonDocument.parse(json);
        if (!(root instanceof List<?> values)) throw invalid("Library root must be an array");
        List<LibraryEntry> entries = new ArrayList<>();
        for (Object value : values) entries.add(entryFromMap(object(value, "library entry")));
        return List.copyOf(entries);
    }

    private static Map<String, Object> entryToMap(LibraryEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entry.id()); map.put("savedAt", entry.savedAt().toString());
        map.put("result", resultToMap(entry.result()));
        map.put("rules", entry.rules().stream().map(Enum::name).toList());
        map.put("customAvoid", entry.customAvoid()); map.put("sourceSummary", entry.sourceSummary());
        map.put("paywalledConversion", entry.isPaywalledConversion());
        return map;
    }

    private static LibraryEntry entryFromMap(Map<String, Object> map) {
        return new LibraryEntry(text(map, "id"), Instant.parse(text(map, "savedAt")),
            resultFromMap(object(map.get("result"), "result")),
            array(map, "rules").stream().map(value -> RuleId.valueOf(string(value, "rule"))).toList(),
            text(map, "customAvoid"), text(map, "sourceSummary"), bool(map, "paywalledConversion"));
    }

    private static Map<String, Object> resultToMap(ConversionResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", result.title()); map.put("servings", result.servings());
        map.put("ingredients", result.ingredients().stream().map(LibraryEntryJsonCodec::ingredientToMap).toList());
        map.put("steps", result.steps().stream().map(LibraryEntryJsonCodec::stepToMap).toList());
        map.put("swaps", result.swaps().stream().map(LibraryEntryJsonCodec::swapToMap).toList());
        map.put("unfixable", result.unfixable());
        Map<String, Object> compliance = new LinkedHashMap<>();
        result.ruleCompliance().forEach((key, value) -> compliance.put(key, value.name()));
        map.put("ruleCompliance", compliance);
        return map;
    }

    private static ConversionResult resultFromMap(Map<String, Object> map) {
        Map<String, RuleCompliance> compliance = new LinkedHashMap<>();
        object(map.get("ruleCompliance"), "ruleCompliance").forEach((key, value) ->
            compliance.put(key, RuleCompliance.valueOf(string(value, "compliance"))));
        return new ConversionResult(text(map, "title"), text(map, "servings"),
            array(map, "ingredients").stream().map(value -> ingredientFromMap(object(value, "ingredient"))).toList(),
            array(map, "steps").stream().map(value -> stepFromMap(object(value, "step"))).toList(),
            array(map, "swaps").stream().map(value -> swapFromMap(object(value, "swap"))).toList(),
            array(map, "unfixable").stream().map(value -> string(value, "unfixable item")).toList(), compliance);
    }

    private static Map<String, Object> ingredientToMap(ConvertedIngredient ingredient) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", ingredient.text()); map.put("changed", ingredient.changed());
        map.put("original", ingredient.original().orElse(null)); map.put("reason", ingredient.reason().orElse(null));
        map.put("ratioNote", ingredient.ratioNote().orElse(null));
        map.put("originalQuantity", ingredient.originalQuantity().orElse(null));
        map.put("convertedQuantity", ingredient.convertedQuantity().orElse(null));
        map.put("unit", ingredient.unit().orElse(null));
        map.put("appliedSwap", ingredient.appliedSwap().map(LibraryEntryJsonCodec::swapToMap).orElse(null));
        return map;
    }

    private static ConvertedIngredient ingredientFromMap(Map<String, Object> map) {
        return new ConvertedIngredient(text(map, "text"), bool(map, "changed"), optionalText(map, "original"),
            optionalText(map, "reason"), optionalText(map, "ratioNote"), optionalDecimal(map, "originalQuantity"),
            optionalDecimal(map, "convertedQuantity"), optionalText(map, "unit"),
            Optional.ofNullable(map.get("appliedSwap")).map(value -> swapFromMap(object(value, "appliedSwap"))));
    }

    private static Map<String, Object> stepToMap(ConvertedStep step) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", step.text()); map.put("changed", step.changed());
        map.put("original", step.original().orElse(null)); map.put("reason", step.reason().orElse(null));
        return map;
    }

    private static ConvertedStep stepFromMap(Map<String, Object> map) {
        return new ConvertedStep(text(map, "text"), bool(map, "changed"), optionalText(map, "original"),
            optionalText(map, "reason"));
    }

    private static Map<String, Object> swapToMap(Swap swap) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("from", swap.from()); map.put("to", swap.to()); map.put("why", swap.why());
        map.put("ratio", swap.ratio()); map.put("category", swap.category().map(Enum::name).orElse(null));
        return map;
    }

    private static Swap swapFromMap(Map<String, Object> map) {
        return new Swap(text(map, "from"), text(map, "to"), text(map, "why"), text(map, "ratio"),
            optionalText(map, "category").map(SwapCategory::valueOf));
    }

    private static Map<String, Object> object(Object value, String name) {
        if (!(value instanceof Map<?, ?> raw)) throw invalid(name + " must be an object");
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, child) -> { if (key instanceof String text) result.put(text, child); });
        return result;
    }
    private static List<?> array(Map<String, Object> map, String key) {
        if (!(map.get(key) instanceof List<?> list)) throw invalid(key + " must be an array");
        return list;
    }
    private static String text(Map<String, Object> map, String key) { return string(map.get(key), key); }
    private static String string(Object value, String name) {
        if (!(value instanceof String text)) throw invalid(name + " must be a string"); return text;
    }
    private static boolean bool(Map<String, Object> map, String key) {
        if (!(map.get(key) instanceof Boolean value)) throw invalid(key + " must be a boolean"); return value;
    }
    private static Optional<String> optionalText(Map<String, Object> map, String key) {
        Object value = map.get(key); return value == null ? Optional.empty() : Optional.of(string(value, key));
    }
    private static Optional<BigDecimal> optionalDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return Optional.empty();
        if (!(value instanceof BigDecimal decimal)) throw invalid(key + " must be a number");
        return Optional.of(decimal);
    }
    private static RecipeLibraryException invalid(String message) {
        return new RecipeLibraryException("Invalid recipe library: " + message);
    }
}
