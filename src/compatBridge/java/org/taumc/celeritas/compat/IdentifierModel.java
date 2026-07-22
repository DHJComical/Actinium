package org.taumc.celeritas.compat;

import java.util.Objects;

/**
 * Neutral option identifier shared by the current and legacy model mappers.
 */
record IdentifierModel<T>(String namespace, String path, Class<T> type) {
    IdentifierModel {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(type, "type");
    }
}
