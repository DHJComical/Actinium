package org.taumc.celeritas.api.options.binding;

/**
 * Reads and writes an option value on its legacy storage object.
 */
public interface OptionBinding<S, T> {
    /**
     * Applies a value to the supplied storage object.
     */
    void setValue(S storage, T value);

    /**
     * Reads the current value from the supplied storage object.
     */
    T getValue(S storage);
}
