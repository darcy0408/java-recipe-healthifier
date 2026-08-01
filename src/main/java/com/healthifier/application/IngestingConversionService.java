package com.healthifier.application;

import com.healthifier.domain.ConversionResult;
import com.healthifier.domain.ConvertInput;
import java.util.List;
import java.util.Objects;

public final class IngestingConversionService implements ConversionService {
    private final ConversionService delegate;
    private final List<RecipeSourceReader> readers;

    public IngestingConversionService(ConversionService delegate, List<RecipeSourceReader> readers) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.readers = List.copyOf(Objects.requireNonNull(readers, "readers"));
    }

    @Override
    public ConversionResult convert(ConversionRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.input() instanceof ConvertInput.Text) return delegate.convert(request);

        RecipeSourceReader reader = readers.stream()
            .filter(candidate -> candidate.supports(request.input()))
            .findFirst()
            .orElseThrow(() -> new UnsupportedOperationException(
                "No recipe source reader supports " + request.input().getClass().getSimpleName()));
        ConvertInput.Text text = reader.read(request.input());
        return delegate.convert(new ConversionRequest(text, request.rules(), request.customAvoid()));
    }
}
