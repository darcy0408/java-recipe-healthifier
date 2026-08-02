package com.healthifier.infrastructure;

import com.healthifier.application.SwapRuleRepository;
import com.healthifier.domain.HealthRule;
import com.healthifier.domain.RuleId;
import com.healthifier.domain.SwapCategory;
import com.healthifier.domain.SwapRule;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class InMemorySwapRuleRepository implements SwapRuleRepository {
    private final List<SwapRule> swaps;
    private final Map<RuleId, HealthRule> healthRules;

    public InMemorySwapRuleRepository() {
        this(defaultSwaps(), defaultHealthRules());
    }

    public InMemorySwapRuleRepository(List<SwapRule> swaps, Map<RuleId, HealthRule> healthRules) {
        this.swaps = List.copyOf(Objects.requireNonNull(swaps, "swaps"));
        EnumMap<RuleId, HealthRule> copy = new EnumMap<>(RuleId.class);
        copy.putAll(Objects.requireNonNull(healthRules, "healthRules"));
        for (RuleId id : RuleId.values()) {
            if (!copy.containsKey(id)) throw new IllegalArgumentException("Missing health rule " + id);
        }
        this.healthRules = Map.copyOf(copy);
    }

    @Override
    public List<SwapRule> findByCategories(Set<SwapCategory> categories) {
        Objects.requireNonNull(categories, "categories");
        return swaps.stream().filter(rule -> rule.categories().stream().anyMatch(categories::contains))
            .sorted(Comparator.comparingInt((SwapRule rule) -> rule.source().length()).reversed()
                .thenComparing(Comparator.comparingInt((SwapRule rule) ->
                    (int) rule.categories().stream().filter(categories::contains).count()).reversed()))
            .toList();
    }

    @Override
    public HealthRule findHealthRule(RuleId ruleId) {
        return healthRules.get(Objects.requireNonNull(ruleId, "ruleId"));
    }

    private static List<SwapRule> defaultSwaps() {
        return List.of(
            swap("panko breadcrumbs", "gluten-free crumb coating", "Removes wheat-based crumbs", "1:1",
                SwapCategory.GLUTEN_FREE),
            swap("whole wheat flour", "almond flour", "Removes gluten-containing wheat", "Start with 1:1; adjust moisture",
                SwapCategory.GLUTEN_FREE),
            swap("all-purpose flour", "almond flour", "Removes gluten and reduces carbohydrates",
                "Start with 1:1; adjust moisture", SwapCategory.GLUTEN_FREE, SwapCategory.KETO,
                SwapCategory.LOW_CARB),
            swap("bread flour", "almond flour", "Removes gluten-containing wheat", "Start with 1:1; adjust moisture",
                SwapCategory.GLUTEN_FREE),
            swap("wheat flour", "almond flour", "Removes gluten-containing wheat", "Start with 1:1; adjust moisture",
                SwapCategory.GLUTEN_FREE),
            swap("soy sauce", "certified gluten-free tamari", "Removes wheat commonly found in soy sauce", "1:1",
                SwapCategory.GLUTEN_FREE),
            swap("breadcrumbs", "gluten-free crumb coating", "Removes wheat-based crumbs", "1:1",
                SwapCategory.GLUTEN_FREE),
            swap("spaghetti", "zucchini noodles", "Removes wheat pasta and reduces carbohydrates", "1:1",
                SwapCategory.GLUTEN_FREE, SwapCategory.KETO, SwapCategory.LOW_CARB),
            swap("pasta", "zucchini noodles", "Removes wheat pasta and reduces carbohydrates", "1:1",
                SwapCategory.GLUTEN_FREE, SwapCategory.KETO, SwapCategory.LOW_CARB),
            swap("pasta", "chickpea noodles", "Removes wheat pasta and adds protein", "1:1",
                SwapCategory.GLUTEN_FREE, SwapCategory.HIGH_PROTEIN),
            swap("semolina", "chickpea flour", "Removes gluten-containing durum wheat", "1:1",
                SwapCategory.GLUTEN_FREE),

            swap("vegetable shortening", "avocado oil", "Avoids industrial seed-oil shortening", "Use 3/4 as much",
                SwapCategory.NO_SEED_OILS, SwapCategory.NO_UPF),
            swap("vegetable oil", "avocado oil", "Avoids industrial seed oils", "1:1",
                SwapCategory.NO_SEED_OILS),
            swap("canola oil", "avocado oil", "Avoids industrial seed oils", "1:1",
                SwapCategory.NO_SEED_OILS),
            swap("soybean oil", "avocado oil", "Avoids industrial seed oils", "1:1",
                SwapCategory.NO_SEED_OILS),
            swap("corn oil", "avocado oil", "Avoids industrial seed oils", "1:1",
                SwapCategory.NO_SEED_OILS),
            swap("sunflower oil", "avocado oil", "Avoids industrial seed oils", "1:1",
                SwapCategory.NO_SEED_OILS),
            swap("safflower oil", "avocado oil", "Avoids industrial seed oils", "1:1",
                SwapCategory.NO_SEED_OILS),
            swap("grapeseed oil", "avocado oil", "Avoids industrial seed oils", "1:1",
                SwapCategory.NO_SEED_OILS),
            swap("margarine", "avocado oil", "Avoids highly processed seed-oil spread", "Use 3/4 as much",
                SwapCategory.NO_SEED_OILS, SwapCategory.NO_UPF, SwapCategory.DAIRY_FREE),

            swap("brown sugar", "allulose", "Reduces added sugar and digestible carbohydrates", "Use slightly less",
                SwapCategory.KETO, SwapCategory.LOW_CARB),
            swap("powdered sugar", "powdered allulose", "Reduces added sugar and digestible carbohydrates", "1:1",
                SwapCategory.KETO, SwapCategory.LOW_CARB),
            swap("white sugar", "allulose", "Reduces added sugar and digestible carbohydrates", "1:1",
                SwapCategory.KETO, SwapCategory.LOW_CARB),
            swap("sugar", "allulose", "Reduces added sugar and digestible carbohydrates", "1:1",
                SwapCategory.KETO, SwapCategory.LOW_CARB),
            swap("maple syrup", "sugar-free maple-style syrup", "Reduces added sugar", "1:1",
                SwapCategory.KETO, SwapCategory.LOW_CARB),
            swap("honey", "allulose syrup", "Reduces added sugar", "1:1",
                SwapCategory.KETO, SwapCategory.LOW_CARB),

            swap("heavy cream", "full-fat coconut cream", "Removes dairy", "1:1",
                SwapCategory.DAIRY_FREE),
            swap("parmesan cheese", "dairy-free parmesan alternative", "Removes dairy cheese", "1:1",
                SwapCategory.DAIRY_FREE),
            swap("cheddar cheese", "dairy-free cheddar alternative", "Removes dairy cheese", "1:1",
                SwapCategory.DAIRY_FREE),
            swap("whole milk", "unsweetened soy beverage", "Removes dairy milk", "1:1",
                SwapCategory.DAIRY_FREE),
            swap("whole milk", "unsweetened almond beverage", "Removes dairy and reduces carbohydrates", "1:1",
                SwapCategory.DAIRY_FREE, SwapCategory.KETO, SwapCategory.LOW_CARB),
            swap("milk", "unsweetened soy beverage", "Removes dairy milk", "1:1",
                SwapCategory.DAIRY_FREE),
            swap("milk", "unsweetened almond beverage", "Removes dairy and reduces carbohydrates", "1:1",
                SwapCategory.DAIRY_FREE, SwapCategory.KETO, SwapCategory.LOW_CARB),
            swap("butter", "extra-virgin olive oil", "Removes dairy fat", "Use 3/4 as much",
                SwapCategory.DAIRY_FREE),

            swap("panko breadcrumbs", "crushed pork rinds", "Removes grain-based ingredients", "1:1",
                SwapCategory.CARNIVORE, SwapCategory.KETO, SwapCategory.LOW_CARB,
                SwapCategory.GLUTEN_FREE),
            swap("breadcrumbs", "crushed pork rinds", "Removes grain-based ingredients", "1:1",
                SwapCategory.CARNIVORE, SwapCategory.KETO, SwapCategory.LOW_CARB,
                SwapCategory.GLUTEN_FREE),
            swap("sour cream", "plain Greek yogurt", "Adds protein while preserving creaminess", "1:1",
                SwapCategory.HIGH_PROTEIN),
            swap("protein bar", "plain Greek yogurt", "Replaces an ultra-processed protein source",
                "Use an equal serving", SwapCategory.NO_UPF, SwapCategory.HIGH_PROTEIN)
        );
    }

    private static Map<RuleId, HealthRule> defaultHealthRules() {
        EnumMap<RuleId, HealthRule> rules = new EnumMap<>(RuleId.class);
        rules.put(RuleId.NO_SEED_OILS, restriction(RuleId.NO_SEED_OILS,
            "vegetable oil", "canola oil", "soybean oil", "corn oil", "sunflower oil",
            "safflower oil", "grapeseed oil", "rice bran oil", "cottonseed oil", "margarine",
            "vegetable shortening"));
        rules.put(RuleId.GLUTEN_FREE, restriction(RuleId.GLUTEN_FREE,
            "all-purpose flour", "whole wheat flour", "wheat flour", "bread flour", "panko",
            "breadcrumbs", "spaghetti", "wheat pasta", "soy sauce", "barley", "semolina", "couscous"));
        rules.put(RuleId.DAIRY_FREE, restriction(RuleId.DAIRY_FREE,
            "butter", "milk", "heavy cream", "sour cream", "parmesan cheese", "cheddar cheese",
            "mozzarella cheese", "yogurt", "whey", "casein"));
        rules.put(RuleId.KETO, restriction(RuleId.KETO,
            "sugar", "brown sugar", "powdered sugar", "honey", "maple syrup", "wheat flour",
            "all-purpose flour", "breadcrumbs", "panko", "spaghetti", "pasta", "chickpea noodles",
            "rice", "potatoes"));
        rules.put(RuleId.LOW_CARB, restriction(RuleId.LOW_CARB,
            "sugar", "brown sugar", "powdered sugar", "honey", "maple syrup", "wheat flour",
            "all-purpose flour", "breadcrumbs", "panko", "spaghetti", "pasta", "chickpea noodles"));
        rules.put(RuleId.CARNIVORE, restriction(RuleId.CARNIVORE,
            "flour", "breadcrumbs", "panko", "sugar", "fruit", "vegetables", "rice", "pasta",
            "beans", "soy", "nuts", "seeds"));
        rules.put(RuleId.NO_UPF, restriction(RuleId.NO_UPF,
            "protein bar", "margarine", "vegetable shortening", "processed cheese",
            "instant pudding mix"));
        rules.put(RuleId.HIGH_PROTEIN, new HealthRule(RuleId.HIGH_PROTEIN, Set.of(),
            Set.of("chicken", "turkey", "beef", "pork", "fish", "salmon", "tuna", "eggs", "egg",
                "Greek yogurt", "cottage cheese", "tofu", "tempeh", "lentils", "protein powder"), true));
        return rules;
    }

    private static HealthRule restriction(RuleId id, String... violations) {
        return new HealthRule(id, Set.of(violations), Set.of(), false);
    }

    private static SwapRule swap(String source, String replacement, String reason, String ratio,
                                 SwapCategory... categories) {
        return new SwapRule(source, replacement, reason, ratio, Set.of(categories));
    }
}
