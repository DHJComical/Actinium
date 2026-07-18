package net.caffeinemc.mods.sodium.api.config.option;

import java.util.function.Supplier;

/**
 * Integer validator that also describes the steps rendered by a numeric control.
 */
public interface SteppedValidator extends Validator<Integer> {
    /** Minimum accepted value. */
    int min();

    /** Maximum accepted value. */
    int max();

    /** Positive interval between accepted values. */
    int step();

    /** Returns whether the value belongs to this stepped range. */
    default boolean isValueValid(int value) {
        return value >= this.min() && value <= this.max() && (value - this.min()) % this.step() == 0;
    }

    @Override
    default Integer getValidatedValue(Integer value, Supplier<Integer> defaultValueSupplier) {
        return this.isValueValid(value) ? value : defaultValueSupplier.get();
    }
}
