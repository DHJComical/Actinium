package org.taumc.celeritas.api.options.control;

import org.embeddedt.embeddium.impl.util.Dim2i;
import org.taumc.celeritas.api.options.structure.Option;

/**
 * Legacy control contract; rendering adapters consume its option metadata.
 */
public interface Control<T> {
    /**
     * Returns the option controlled by this legacy widget.
     */
    Option<T> getOption();

    /**
     * Creates the upstream-compatible interactive widget for the supplied bounds.
     */
    ControlElement<T> createElement(Dim2i dim);

    /**
     * Returns the horizontal space reserved for the control value.
     */
    int getMaxWidth();
}
