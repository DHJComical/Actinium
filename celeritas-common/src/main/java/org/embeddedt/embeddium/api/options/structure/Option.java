package org.embeddedt.embeddium.api.options.structure;

import org.embeddedt.embeddium.api.options.control.Control;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface Option<T> {
    @Nullable
    default OptionIdentifier<T> getId() {
        return null;
    }

    TextComponent getName();

    TextComponent getTooltip();

    OptionImpact getImpact();

    Control<T> getControl();

    T getValue();

    void setValue(T value);

    void reset();

    OptionStorage<?> getStorage();

    boolean isAvailable();

    boolean hasChanged();

    void applyChanges();

    Collection<OptionFlag> getFlags();

    /**
     * Actinium extension: returns the last applied (binding-backed) value.
     * The default implementation falls back to the current value for
     * controls that do not track a separate applied baseline.
     */
    default T getAppliedValue() {
        return this.getValue();
    }

    /**
     * Actinium extension: returns the declared default value, or the current
     * value when the implementation does not track defaults.
     */
    default T getDefaultValue() {
        return this.getValue();
    }

    /**
     * Actinium extension: moves the pending value back to the declared
     * default. The default implementation resets pending changes, leaving
     * the binding value untouched.
     */
    default void resetToDefault() {
        this.reset();
    }

    /**
     * Actinium extension: returns whether this option's control should be
     * hidden while the option is disabled.
     */
    default boolean shouldHideControl() {
        return false;
    }
}
