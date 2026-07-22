package org.taumc.celeritas.api;

import org.taumc.celeritas.api.eventbus.EmbeddiumEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;
import org.taumc.celeritas.api.options.structure.OptionPage;

import java.util.List;

/**
 * Legacy page construction event. The shared bus is dispatched by Actinium's GUI bridge.
 */
public class OptionGUIConstructionEvent extends EmbeddiumEvent {

    public static final EventHandlerRegistrar<OptionGUIConstructionEvent> BUS = new EventHandlerRegistrar<>();
    private final List<OptionPage> pages;

    public OptionGUIConstructionEvent(List<OptionPage> pages) {
        this.pages = pages;
    }

    public List<OptionPage> getPages() {
        return this.pages;
    }

    public void addPage(OptionPage page) {
        pages.add(page);
    }

}
