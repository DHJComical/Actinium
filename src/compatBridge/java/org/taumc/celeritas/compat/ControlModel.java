package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Neutral control description used to cross the current/legacy package boundary without reflection.
 */
interface ControlModel<T> {
}

final class TickBoxModel implements ControlModel<Boolean> {
    static final TickBoxModel INSTANCE = new TickBoxModel();

    private TickBoxModel() {
    }
}

record SliderModel(int min, int max, int interval,
                   IntFunction<TextComponent> formatter) implements ControlModel<Integer> {
    SliderModel {
        Objects.requireNonNull(formatter, "formatter");
    }
}

final class CyclingModel<T> implements ControlModel<T> {
    private final T[] allowedValues;
    private final TextComponent[] names;

    CyclingModel(T[] allowedValues, TextComponent[] names) {
        if (allowedValues.length != names.length || allowedValues.length == 0) {
            throw new IllegalArgumentException("Cycling values and names must have equal non-zero length");
        }
        this.allowedValues = allowedValues.clone();
        this.names = names.clone();
    }

    T[] allowedValues() {
        return allowedValues.clone();
    }

    TextComponent[] names() {
        return names.clone();
    }
}
