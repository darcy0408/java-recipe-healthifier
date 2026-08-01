package com.healthifier.infrastructure;

public final class RecipeIngestionException extends RuntimeException {
    public RecipeIngestionException(String message) {
        super(message);
    }

    public RecipeIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
