package com.healthifier.application;

import com.healthifier.domain.ConversionResult;
import com.healthifier.domain.ConvertInput;
import com.healthifier.domain.ConvertedIngredient;
import com.healthifier.domain.ConvertedStep;
import com.healthifier.domain.RuleCompliance;
import com.healthifier.domain.RuleId;
import com.healthifier.domain.Swap;
import com.healthifier.domain.SwapCategory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextRecipeConversionService implements ConversionService {
    private static final Pattern SERVINGS = Pattern.compile("(?i)^servings?\\s*:\\s*(.+)$");
    private static final Pattern LIST_PREFIX = Pattern.compile("^\\s*(?:[-*]|\\d+[.)])\\s+");

    private final SwapRuleRepository rules;

    public TextRecipeConversionService(SwapRuleRepository rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    @Override
    public ConversionResult convert(ConversionRequest request) {
        Objects.requireNonNull(request, "request");
        if (!(request.input() instanceof ConvertInput.Text textInput)) {
            throw new UnsupportedOperationException("Only text recipe conversion is currently supported");
        }

        ParsedRecipe recipe = parse(textInput.text());
        Set<SwapCategory> categories = request.rules().stream()
            .map(RuleId::category)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        List<Swap> candidates = rules.findByCategories(categories);
        List<Swap> applied = new ArrayList<>();
        List<String> unfixable = new ArrayList<>();
        List<ConvertedIngredient> ingredients = recipe.ingredients().stream()
            .map(line -> convertIngredient(line, candidates, request.customAvoid(), applied, unfixable))
            .toList();

        Map<String, RuleCompliance> compliance = new LinkedHashMap<>();
        request.rules().forEach(rule -> compliance.put(rule.name(), RuleCompliance.COMPLIANT));
        request.customAvoid().ifPresent(value -> compliance.put("CUSTOM_AVOID",
            unfixable.isEmpty() ? RuleCompliance.COMPLIANT : RuleCompliance.NOT_POSSIBLE));

        return ConversionResult.builder()
            .title(recipe.title())
            .servings(recipe.servings())
            .ingredients(ingredients)
            .steps(recipe.steps().stream().map(step -> new ConvertedStep(step, false)).toList())
            .swaps(List.copyOf(new LinkedHashSet<>(applied)))
            .unfixable(unfixable)
            .ruleCompliance(compliance)
            .build();
    }

    private static ConvertedIngredient convertIngredient(
        String ingredient,
        List<Swap> candidates,
        Optional<String> customAvoid,
        List<Swap> applied,
        List<String> unfixable
    ) {
        for (Swap swap : candidates) {
            Pattern source = phrasePattern(swap.from());
            Matcher matcher = source.matcher(ingredient);
            if (matcher.find()) {
                String converted = matcher.replaceAll(Matcher.quoteReplacement(swap.to()));
                applied.add(swap);
                return new ConvertedIngredient(converted, true, Optional.of(ingredient),
                    Optional.of(swap.why()), Optional.of(swap.ratio()), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.of(swap));
            }
        }

        customAvoid.filter(value -> phrasePattern(value).matcher(ingredient).find())
            .ifPresent(value -> unfixable.add("No substitution found for '" + value
                + "' in ingredient: " + ingredient));
        return new ConvertedIngredient(ingredient, false, Optional.empty(), Optional.empty(),
            Optional.empty());
    }

    private static Pattern phrasePattern(String phrase) {
        return Pattern.compile("(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(phrase)
            + "(?![\\p{L}\\p{N}])");
    }

    private static ParsedRecipe parse(String source) {
        List<String> lines = source.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
        if (lines.isEmpty()) throw new IllegalArgumentException("Recipe text must not be empty");

        String title = lines.getFirst();
        String servings = "Not specified";
        Section section = Section.NONE;
        List<String> ingredients = new ArrayList<>();
        List<String> steps = new ArrayList<>();

        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            Matcher servingsMatcher = SERVINGS.matcher(line);
            if (servingsMatcher.matches()) {
                servings = servingsMatcher.group(1).strip();
            } else if (isHeading(line, "ingredients")) {
                section = Section.INGREDIENTS;
            } else if (isHeading(line, "instructions") || isHeading(line, "directions")
                    || isHeading(line, "steps")) {
                section = Section.STEPS;
            } else if (section == Section.INGREDIENTS) {
                ingredients.add(LIST_PREFIX.matcher(line).replaceFirst(""));
            } else if (section == Section.STEPS) {
                steps.add(LIST_PREFIX.matcher(line).replaceFirst(""));
            }
        }
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("Recipe text requires an Ingredients section");
        }
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Recipe text requires an Instructions, Directions, or Steps section");
        }
        return new ParsedRecipe(title, servings, ingredients, steps);
    }

    private static boolean isHeading(String line, String heading) {
        return line.replaceFirst(":$", "").strip().toLowerCase(Locale.ROOT).equals(heading);
    }

    private enum Section { NONE, INGREDIENTS, STEPS }

    private record ParsedRecipe(String title, String servings, List<String> ingredients,
                                List<String> steps) {}
}
