package net.caffeinemc.mods.sodium.api.config.option;

/**
 * Connects an option model to an existing configuration value.
 *
 * @param <V> option value type
 */
public interface OptionBinding<V> {
    /**
     * Validates and optionally normalizes a pending value before any binding is written.
     */
    default V validate(V value) {
        return value;
    }

    /** Writes a previously validated value to the existing configuration source. */
    void save(V value);

    /** Loads the current value from the existing configuration source. */
    V load();
}
