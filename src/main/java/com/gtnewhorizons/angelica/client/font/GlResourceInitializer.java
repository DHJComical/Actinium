package com.gtnewhorizons.angelica.client.font;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Lazily creates a shared OpenGL resource only for a thread with a current context.
 *
 * @param <T> resource type published to all font renderer instances
 */
final class GlResourceInitializer<T> {

    private final BooleanSupplier currentContext;
    private final Supplier<T> factory;
    private volatile T resource;

    GlResourceInitializer(BooleanSupplier currentContext, Supplier<T> factory) {
        this.currentContext = Objects.requireNonNull(currentContext, "currentContext");
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    /**
     * Returns the shared resource, creating it once after validating the caller's OpenGL context.
     */
    T get() {
        if (!currentContext.getAsBoolean()) {
            throw new IllegalStateException("Font rendering requires a current OpenGL context");
        }

        T initialized = resource;
        if (initialized != null) {
            return initialized;
        }

        synchronized (this) {
            initialized = resource;
            if (initialized == null) {
                initialized = Objects.requireNonNull(factory.get(), "OpenGL resource factory returned null");
                resource = initialized;
            }
        }
        return initialized;
    }
}
