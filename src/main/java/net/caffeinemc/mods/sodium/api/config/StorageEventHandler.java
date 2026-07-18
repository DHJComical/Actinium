package net.caffeinemc.mods.sodium.api.config;

/**
 * Persists an existing configuration source after all bound values have been written.
 */
@FunctionalInterface
public interface StorageEventHandler {
    /** Flushes the associated configuration source exactly once per apply transaction. */
    void afterSave();
}
