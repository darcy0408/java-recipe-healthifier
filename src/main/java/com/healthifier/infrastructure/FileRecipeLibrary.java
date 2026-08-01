package com.healthifier.infrastructure;

import com.healthifier.application.RecipeLibrary;
import com.healthifier.domain.LibraryEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class FileRecipeLibrary implements RecipeLibrary {
    private final Path file;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public FileRecipeLibrary(Path file) { this.file = Objects.requireNonNull(file, "file").toAbsolutePath(); }

    @Override
    public void save(LibraryEntry entry) {
        Objects.requireNonNull(entry, "entry");
        lock.writeLock().lock();
        try {
            List<LibraryEntry> entries = new ArrayList<>(readEntries());
            if (entries.stream().anyMatch(existing -> existing.id().equals(entry.id()))) {
                throw new RecipeLibraryException("A library entry already exists with id " + entry.id());
            }
            entries.add(entry);
            writeEntries(entries);
        } finally { lock.writeLock().unlock(); }
    }

    @Override
    public Optional<LibraryEntry> findById(String id) {
        Objects.requireNonNull(id, "id");
        lock.readLock().lock();
        try { return readEntries().stream().filter(entry -> entry.id().equals(id)).findFirst(); }
        finally { lock.readLock().unlock(); }
    }

    @Override
    public List<LibraryEntry> findAll() {
        lock.readLock().lock();
        try { return readEntries().stream().sorted(Comparator.comparing(LibraryEntry::savedAt).reversed()).toList(); }
        finally { lock.readLock().unlock(); }
    }

    @Override
    public boolean deleteById(String id) {
        Objects.requireNonNull(id, "id");
        lock.writeLock().lock();
        try {
            List<LibraryEntry> entries = new ArrayList<>(readEntries());
            boolean removed = entries.removeIf(entry -> entry.id().equals(id));
            if (removed) writeEntries(entries);
            return removed;
        } finally { lock.writeLock().unlock(); }
    }

    private List<LibraryEntry> readEntries() {
        if (Files.notExists(file)) return List.of();
        try { return LibraryEntryJsonCodec.decode(Files.readString(file, StandardCharsets.UTF_8)); }
        catch (IOException | IllegalArgumentException exception) {
            throw new RecipeLibraryException("Unable to read recipe library " + file, exception);
        }
    }

    private void writeEntries(List<LibraryEntry> entries) {
        Path parent = file.getParent();
        try {
            if (parent != null) Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
            try {
                Files.writeString(temporary, LibraryEntryJsonCodec.encode(entries), StandardCharsets.UTF_8);
                try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException exception) {
            throw new RecipeLibraryException("Unable to write recipe library " + file, exception);
        }
    }
}
