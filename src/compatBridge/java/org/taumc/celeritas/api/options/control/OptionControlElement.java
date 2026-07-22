package org.taumc.celeritas.api.options.control;

import org.taumc.celeritas.api.options.structure.Option;

/**
 * Exposes the option represented by a legacy GUI control element.
 */
public interface OptionControlElement<T> {
    /**
     * Returns the represented option.
     */
    Option<T> getOption();
}
