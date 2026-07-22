package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.List;

/**
 * Neutral ordered option page used across API package boundaries.
 */
record OptionPageModel(IdentifierModel<Void> id, TextComponent name, List<OptionGroupModel> groups) {
    OptionPageModel {
        groups = List.copyOf(groups);
    }
}
