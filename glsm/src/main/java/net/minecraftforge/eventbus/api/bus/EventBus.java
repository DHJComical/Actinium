package net.minecraftforge.eventbus.api.bus;

import net.minecraftforge.eventbus.api.event.MutableEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class EventBus<T extends MutableEvent> {
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    private EventBus() {
    }

    public static <T extends MutableEvent> EventBus<T> create(Class<T> eventType) {
        return new EventBus<>();
    }

    public void addListener(Consumer<T> listener) {
        listeners.add(listener);
    }

    public boolean hasListeners() {
        return !listeners.isEmpty();
    }

    public void post(T event) {
        for (Consumer<T> listener : listeners) {
            listener.accept(event);
        }
    }
}
