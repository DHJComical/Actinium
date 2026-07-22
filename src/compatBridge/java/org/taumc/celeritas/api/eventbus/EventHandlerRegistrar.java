package org.taumc.celeritas.api.eventbus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Standalone legacy event registrar used by Celeritas addons.
 */
public class EventHandlerRegistrar<T extends EmbeddiumEvent> {
    private final List<Handler<T>> handlers = new CopyOnWriteArrayList<>();

    public void addListener(Handler<T> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Legacy event handler must not be null");
        }
        handlers.add(handler);
    }

    public void removeListener(Handler<T> handler) {
        handlers.remove(handler);
    }

    public boolean post(T event) {
        for (Handler<T> handler : handlers) {
            handler.acceptEvent(event);
        }
        return event.isCancelable() && event.isCanceled();
    }

    @FunctionalInterface
    public interface Handler<T extends EmbeddiumEvent> {
        /** Receives a legacy Celeritas event posted by this registrar. */
        void acceptEvent(T event);
    }
}
