package com.healthifier.application;

import com.healthifier.domain.LibraryEntry;
import java.util.List;
import java.util.Optional;

public interface RecipeLibrary {
    void save(LibraryEntry entry);

    Optional<LibraryEntry> findById(String id);

    List<LibraryEntry> findAll();

    boolean deleteById(String id);
}
