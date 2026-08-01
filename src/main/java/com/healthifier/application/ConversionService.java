package com.healthifier.application;

import com.healthifier.domain.ConversionResult;

@FunctionalInterface
public interface ConversionService {
    ConversionResult convert(ConversionRequest request);
}
