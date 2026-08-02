package com.healthifier.application;

import com.healthifier.domain.HealthRule;
import com.healthifier.domain.RuleId;
import com.healthifier.domain.Swap;
import com.healthifier.domain.SwapCategory;
import com.healthifier.domain.SwapRule;
import java.util.List;
import java.util.Set;

public interface SwapRuleRepository {
    List<SwapRule> findByCategories(Set<SwapCategory> categories);

    HealthRule findHealthRule(RuleId ruleId);
}
