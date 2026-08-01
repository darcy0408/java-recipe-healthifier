package com.healthifier.infrastructure;

public final class RecipeLibraryException extends RuntimeException {
    public RecipeLibraryException(String message) { super(message); }
    public RecipeLibraryException(String message, Throwable cause) { super(message, cause); }
}
