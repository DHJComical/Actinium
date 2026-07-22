package org.taumc.celeritas.api.options.structure;

import java.util.Set;

/**
 * Persistence boundary used by legacy Celeritas option builders.
 */
public interface OptionStorage<T> {
    /** Returns the mutable configuration object backing this option storage. */
    T getData();

    /** Persists all pending changes. */
    default void save() {
    }

    /** Persists pending changes while preserving their declared update flags. */
    default void save(Set<OptionFlag> flags) {
        save();
    }
}
