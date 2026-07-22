package org.taumc.celeritas.api;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.eventbus.EmbeddiumEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.OptionGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fired when a legacy option page is created so addons can append groups.
 */
public class OptionPageConstructionEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<OptionPageConstructionEvent> BUS = new EventHandlerRegistrar<>();
    private final OptionIdentifier<Void> id;
    private final TextComponent translationKey;
    private final List<OptionGroup> additionalGroups = new ArrayList<>();

    public OptionPageConstructionEvent(OptionIdentifier<Void> id, TextComponent translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public OptionIdentifier<Void> getId() {
        return id;
    }

    public TextComponent getTranslationKey() {
        return translationKey;
    }

    public void addGroup(OptionGroup group) {
        additionalGroups.add(group);
    }

    public List<OptionGroup> getAdditionalGroups() {
        return Collections.unmodifiableList(additionalGroups);
    }
}
