package net.caffeinemc.mods.sodium.api.config;

import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;

/**
 * Explicit extension point used to contribute configuration models before the registry is frozen.
 */
@FunctionalInterface
public interface ConfigEntryPoint {
    default void registerConfigEarly(ConfigBuilder builder) {
    }

    void registerConfigLate(ConfigBuilder builder);
    /**
     * Registers all pages and options owned by this entry point.
     *
     * @param builder root builder scoped to this registration
     */
    default void registerConfig(ConfigBuilder builder) { registerConfigLate(builder); }
}
