package org.taumc.celeritas.compat;

import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionGroup;

import java.util.List;

/**
 * Legacy group projection that bypasses the builder's construction event.
 */
final class LegacyOptionGroupView extends OptionGroup {
    LegacyOptionGroupView(OptionIdentifier<Void> id, List<Option<?>> options) {
        super(id, options);
    }
}
