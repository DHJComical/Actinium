package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.Set;

/**
 * Neutral option contract used by both directions of the Celeritas GUI bridge.
 */
interface OptionModel<T> {
    /**
     * Returns the stable option identifier.
     */
    IdentifierModel<T> id();

    /**
     * Returns the visible option name.
     */
    TextComponent name();

    /**
     * Returns the visible option tooltip.
     */
    TextComponent tooltip();

    /**
     * Returns the shared impact enum name, or null when no impact is declared.
     */
    String impactName();

    /**
     * Returns the control shape and metadata.
     */
    ControlModel<T> control();

    /**
     * Returns the storage behavior with flag names preserved.
     */
    StorageModel<?> storage();

    /**
     * Returns the effective option value.
     */
    T getValue();

    /**
     * Stages a new option value.
     */
    void setValue(T value);

    /**
     * Discards a staged option value.
     */
    void reset();

    /**
     * Reports whether the option can currently be changed.
     */
    boolean isAvailable();

    /**
     * Reports whether a staged value differs from storage.
     */
    boolean hasChanged();

    /**
     * Applies the staged value to storage.
     */
    void applyChanges();

    /**
     * Returns shared names for all option change flags.
     */
    Set<String> flagNames();
}
