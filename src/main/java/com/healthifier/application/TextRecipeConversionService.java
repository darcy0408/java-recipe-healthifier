package com.healthifier.application;

import com.healthifier.domain.ConversionResult;
import com.healthifier.domain.ConvertInput;
import com.healthifier.domain.ConvertedIngredient;
import com.healthifier.domain.ConvertedStep;
import com.healthifier.domain.HealthRule;
import com.healthifier.domain.RuleCompliance;
import com.healthifier.domain.RuleId;
import com.healthifier.domain.Swap;
import com.healthifier.domain.SwapCategory;
import com.healthifier.domain.SwapRule;
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
        List<SwapRule> candidates = rules.findByCategories(categories);
        List<Swap> applied = new ArrayList<>();
        List<String> unfixable = new ArrayList<>();
        List<ConvertedIngredient> ingredients = recipe.ingredients().stream()
            .map(line -> convertIngredient(line, candidates, categories, request.customAvoid(), applied, unfixable))
            .toList();

        List<ConvertedStep> steps = recipe.steps().stream()
            .map(step -> convertStep(step, applied))
            .toList();
        Map<String, RuleCompliance> compliance = evaluateCompliance(request, recipe.ingredients(),
            ingredients, unfixable);

        return ConversionResult.builder()
            .title(recipe.title())
            .servings(recipe.servings())
            .ingredients(ingredients)
            .steps(steps)
            .swaps(List.copyOf(new LinkedHashSet<>(applied)))
            .unfixable(unfixable)
            .ruleCompliance(compliance)
            .build();
    }

    private static ConvertedIngredient convertIngredient(
        String ingredient,
        List<SwapRule> candidates,
        Set<SwapCategory> selectedCategories,
        Optional<String> customAvoid,
        List<Swap> applied,
        List<String> unfixable
    ) {
        String converted = ingredient;
        List<Swap> ingredientSwaps = new ArrayList<>();
        for (SwapRule rule : candidates) {
            Pattern source = phrasePattern(rule.source());
            Matcher matcher = source.matcher(converted);
            if (matcher.find()) {
                converted = matcher.replaceAll(Matcher.quoteReplacement(rule.replacement()));
                SwapCategory appliedCategory = rule.categories().stream()
                    .filter(selectedCategories::contains)
                    .sorted()
                    .findFirst()
                    .orElseThrow();
                Swap swap = rule.toAppliedSwap(appliedCategory);
                applied.add(swap);
                ingredientSwaps.add(swap);
            }
        }

        String finalText = converted;
        customAvoid.filter(value -> phrasePattern(value).matcher(finalText).find())
            .ifPresent(value -> unfixable.add("No substitution found for '" + value
                + "' in ingredient: " + finalText));
        if (ingredientSwaps.isEmpty()) {
            return new ConvertedIngredient(ingredient, false, Optional.empty(), Optional.empty(),
                Optional.empty());
        }
        String reasons = ingredientSwaps.stream().map(Swap::why).distinct()
            .collect(java.util.stream.Collectors.joining("; "));
        String ratios = ingredientSwaps.stream()
            .map(swap -> swap.from() + " → " + swap.to() + ": " + swap.ratio())
            .collect(java.util.stream.Collectors.joining("; "));
        return new ConvertedIngredient(finalText, true, Optional.of(ingredient), Optional.of(reasons),
            Optional.of(ratios), Optional.empty(), Optional.empty(), Optional.empty(), ingredientSwaps);
    }

    private static ConvertedStep convertStep(String step, List<Swap> applied) {
        String converted = step;
        List<Swap> stepSwaps = applied.stream().distinct()
            .sorted(java.util.Comparator.comparingInt((Swap swap) -> swap.from().length()).reversed())
            .toList();
        List<String> changes = new ArrayList<>();
        for (Swap swap : stepSwaps) {
            Matcher matcher = phrasePattern(swap.from()).matcher(converted);
            if (matcher.find()) {
                converted = matcher.replaceAll(Matcher.quoteReplacement(swap.to()));
                changes.add(swap.from() + " → " + swap.to());
            }
        }
        return changes.isEmpty()
            ? new ConvertedStep(step, false)
            : new ConvertedStep(converted, true, Optional.of(step),
                Optional.of("Updated applied substitutions: " + String.join(", ", changes)));
    }

    private Map<String, RuleCompliance> evaluateCompliance(
        ConversionRequest request,
        List<String> originalIngredients,
        List<ConvertedIngredient> convertedIngredients,
        List<String> unfixable
    ) {
        Map<String, RuleCompliance> result = new LinkedHashMap<>();
        List<String> finalIngredients = convertedIngredients.stream().map(ConvertedIngredient::text).toList();
        for (RuleId ruleId : request.rules()) {
            HealthRule rule = rules.findHealthRule(ruleId);
            Set<String> originalViolations = matchingTerms(originalIngredients, rule.violationTerms());
            Set<String> remainingViolations = matchingTerms(finalIngredients, rule.violationTerms());
            RuleCompliance status;
            if (!remainingViolations.isEmpty()) {
                status = remainingViolations.size() < originalViolations.size()
                    ? RuleCompliance.PARTIAL : RuleCompliance.NOT_POSSIBLE;
                remainingViolations.forEach(term -> unfixable.add(ruleId
                    + " still contains '" + term + "'"));
            } else if (rule.evidenceRequired()
                    && matchingTerms(finalIngredients, rule.evidenceTerms()).isEmpty()) {
                status = RuleCompliance.PARTIAL;
                unfixable.add(ruleId + " needs review: no recognized supporting ingredient was found");
            } else {
                status = RuleCompliance.COMPLIANT;
            }
            result.put(ruleId.name(), status);
        }
        request.customAvoid().ifPresent(term -> {
            boolean remains = finalIngredients.stream().anyMatch(text -> phrasePattern(term).matcher(text).find());
            result.put("CUSTOM_AVOID", remains ? RuleCompliance.NOT_POSSIBLE : RuleCompliance.COMPLIANT);
        });
        return result;
    }

    private static Set<String> matchingTerms(List<String> texts, Set<String> terms) {
        return terms.stream().filter(term -> texts.stream()
            .anyMatch(text -> phrasePattern(term).matcher(text).find()))
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
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
