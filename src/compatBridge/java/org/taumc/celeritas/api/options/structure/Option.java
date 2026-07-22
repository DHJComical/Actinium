package org.taumc.celeritas.api.options.structure;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.Control;

import java.util.Collection;

/**
 * Legacy option contract retained for addons compiled against Celeritas.
 */
public interface Option<T> {
    /**
     * Returns the stable identifier, or null for legacy implementations that do not declare one.
     */
    default OptionIdentifier<T> getId() {
        return null;
    }

    /**
     * Returns the visible option name.
     */
    TextComponent getName();

    /**
     * Returns the visible option tooltip.
     */
    TextComponent getTooltip();

    /**
     * Returns the performance impact classification, if declared.
     */
    OptionImpact getImpact();

    /**
     * Returns the widget metadata bound to this option.
     */
    Control<T> getControl();

    /**
     * Returns the staged value or the current stored value.
     */
    T getValue();

    /**
     * Stages a new value for later application.
     */
    void setValue(T value);

    /**
     * Discards the staged value.
     */
    void reset();

    /**
     * Returns the storage object responsible for persistence.
     */
    OptionStorage<?> getStorage();

    /**
     * Reports whether the option can currently be changed.
     */
    boolean isAvailable();

    /**
     * Reports whether the staged value differs from storage.
     */
    boolean hasChanged();

    /**
     * Writes the staged value into storage.
     */
    void applyChanges();

    /**
     * Returns side-effect flags associated with applying the value.
     */
    Collection<OptionFlag> getFlags();
}
