package com.healthifier.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.healthifier.domain.SwapCategory;
import com.healthifier.domain.RuleId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InMemorySwapRuleRepositoryTest {

    @Test
    void returnsOnlyRequestedCategories() {
        var repository = new InMemorySwapRuleRepository();

        var swaps = repository.findByCategories(Set.of(SwapCategory.NO_SEED_OILS));

        assertTrue(swaps.size() >= 2);
        assertTrue(swaps.stream().allMatch(swap ->
            swap.categories().contains(SwapCategory.NO_SEED_OILS)));
    }

    @Test
    void providesAtLeastOneSwapForEveryAdvertisedRule() {
        var repository = new InMemorySwapRuleRepository();

        for (RuleId rule : RuleId.values()) {
            assertTrue(repository.findByCategories(Set.of(rule.category())).size() > 0,
                () -> "Missing swap rules for " + rule);
        }
    }
}
