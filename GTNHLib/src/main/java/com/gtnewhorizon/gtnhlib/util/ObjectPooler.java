package com.gtnewhorizon.gtnhlib.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.function.Supplier;

public final class ObjectPooler<T> {
    private final Supplier<T> instanceSupplier;
    private final ArrayDeque<T> availableInstances = new ArrayDeque<>();

    public ObjectPooler(Supplier<T> instanceSupplier) {
        this.instanceSupplier = instanceSupplier;
    }

    public T getInstance() {
        T instance = availableInstances.pollLast();
        return instance != null ? instance : instanceSupplier.get();
    }

    public void releaseInstance(T instance) {
        if (instance != null) {
            availableInstances.addLast(instance);
        }
    }

    public void releaseInstances(Collection<T> instances) {
        for (T instance : instances) {
            releaseInstance(instance);
        }
        instances.clear();
    }
}
