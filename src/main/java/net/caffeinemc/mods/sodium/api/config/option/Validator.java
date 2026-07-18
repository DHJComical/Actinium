package net.caffeinemc.mods.sodium.api.config.option;

import java.util.function.Supplier;

/**
 * Validates a value and may replace invalid input with the option's declared default.
 */
public interface Validator<V> {
    /** Returns the accepted value or the supplied default. */
    V getValidatedValue(V value, Supplier<V> defaultValueSupplier);
}
