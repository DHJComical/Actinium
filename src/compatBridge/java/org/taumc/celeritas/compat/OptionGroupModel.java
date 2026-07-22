package org.taumc.celeritas.compat;

import java.util.List;

/**
 * Neutral ordered option group used across API package boundaries.
 */
record OptionGroupModel(IdentifierModel<Void> id, List<OptionModel<?>> options) {
    OptionGroupModel {
        options = List.copyOf(options);
    }
}
