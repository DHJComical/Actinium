package org.taumc.celeritas.api;

import org.taumc.celeritas.api.eventbus.EmbeddiumEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.Option;

import java.util.List;

/**
 * Legacy option group construction event used by Celeritas addons.
 */
public class OptionGroupConstructionEvent extends EmbeddiumEvent {

    public static final EventHandlerRegistrar<OptionGroupConstructionEvent> BUS = new EventHandlerRegistrar<>();
    private final OptionIdentifier<Void> id;
    private final List<Option<?>> options;

    public OptionGroupConstructionEvent(OptionIdentifier<Void> id, List<Option<?>> options) {
        this.id = id;
        this.options = options;
    }

    public OptionIdentifier<Void> getId() {
        return this.id;
    }

    public List<Option<?>> getOptions() {
        return this.options;
    }

}
