package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionPage;

import java.util.List;

/**
 * Legacy page projection that intentionally bypasses legacy construction events.
 */
final class LegacyOptionPageView extends OptionPage {
    LegacyOptionPageView(OptionIdentifier<Void> id, TextComponent name, List<OptionGroup> groups) {
        super(id, name, groups, false);
    }
}
