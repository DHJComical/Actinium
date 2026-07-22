package org.taumc.celeritas.compat;

import java.util.Set;

/**
 * Neutral storage contract that preserves save flags while crossing option API generations.
 */
interface StorageModel<T> {
    /**
     * Returns the mutable configuration object bound to the option.
     */
    T getData();

    /**
     * Persists the configuration when no change flags were supplied.
     */
    void save();

    /**
     * Persists the configuration with enum names shared by both option APIs.
     */
    void save(Set<String> flags);
}
