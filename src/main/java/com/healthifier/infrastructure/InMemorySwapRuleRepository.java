package com.healthifier.infrastructure;

import com.healthifier.application.SwapRuleRepository;
import com.healthifier.domain.Swap;
import com.healthifier.domain.SwapCategory;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class InMemorySwapRuleRepository implements SwapRuleRepository {
    private final List<Swap> swaps;

    public InMemorySwapRuleRepository() {
        this(defaultSwaps());
    }

    public InMemorySwapRuleRepository(List<Swap> swaps) {
        this.swaps = List.copyOf(Objects.requireNonNull(swaps, "swaps"));
    }

    @Override
    public List<Swap> findByCategories(Set<SwapCategory> categories) {
        Objects.requireNonNull(categories, "categories");
        return swaps.stream()
            .filter(swap -> swap.category().filter(categories::contains).isPresent())
            .toList();
    }

    private static List<Swap> defaultSwaps() {
        return List.of(
            swap("vegetable oil", "avocado oil", "Avoids industrial seed oils", "1:1",
                SwapCategory.NO_SEED_OILS),
            swap("canola oil", "avocado oil", "Avoids industrial seed oils", "1:1",
                SwapCategory.NO_SEED_OILS),
            swap("all-purpose flour", "almond flour", "Removes gluten-containing wheat flour",
                "Start with 1:1; adjust moisture as needed", SwapCategory.GLUTEN_FREE),
            swap("breadcrumbs", "crushed pork rinds", "Removes grain-based carbohydrates", "1:1",
                SwapCategory.CARNIVORE),
            swap("sugar", "allulose", "Reduces digestible carbohydrates", "1:1",
                SwapCategory.KETO),
            swap("sugar", "allulose", "Reduces digestible carbohydrates", "1:1",
                SwapCategory.LOW_CARB),
            swap("sour cream", "plain Greek yogurt", "Adds protein while preserving creaminess",
                "1:1", SwapCategory.HIGH_PROTEIN),
            swap("heavy cream", "full-fat coconut milk", "Removes dairy", "1:1",
                SwapCategory.DAIRY_FREE),
            swap("protein bar", "plain Greek yogurt", "Replaces an ultra-processed ingredient",
                "Use an equal serving", SwapCategory.NO_UPF)
        );
    }

    private static Swap swap(String from, String to, String why, String ratio,
                             SwapCategory category) {
        return new Swap(from, to, why, ratio, Optional.of(category));
    }
}
