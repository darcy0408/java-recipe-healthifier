package com.healthifier.application;

import com.healthifier.domain.Swap;
import com.healthifier.domain.SwapCategory;
import java.util.List;
import java.util.Set;

public interface SwapRuleRepository {
    List<Swap> findByCategories(Set<SwapCategory> categories);
}
