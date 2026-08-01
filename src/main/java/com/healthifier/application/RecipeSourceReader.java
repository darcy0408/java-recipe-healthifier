package com.healthifier.application;

import com.healthifier.domain.ConvertInput;

public interface RecipeSourceReader {
    boolean supports(ConvertInput input);

    ConvertInput.Text read(ConvertInput input);
}
