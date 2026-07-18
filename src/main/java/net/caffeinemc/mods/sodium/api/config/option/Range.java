package net.caffeinemc.mods.sodium.api.config.option;

/**
 * Immutable stepped integer range used for validation and control construction.
 */
public record Range(int min, int max, int step) implements SteppedValidator {
    public Range {
        if (min > max) {
            throw new IllegalArgumentException("Min must be less than or equal to max");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("Step must be greater than zero");
        }
    }
}
