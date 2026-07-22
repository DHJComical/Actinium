package org.taumc.celeritas.api.options.binding;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Function-backed binding retained under the legacy Celeritas binary name.
 */
public final class GenericBinding<S, T> implements OptionBinding<S, T> {
    private final BiConsumer<S, T> setter;
    private final Function<S, T> getter;

    public GenericBinding(BiConsumer<S, T> setter, Function<S, T> getter) {
        this.setter = Objects.requireNonNull(setter, "setter");
        this.getter = Objects.requireNonNull(getter, "getter");
    }

    @Override
    public void setValue(S storage, T value) {
        setter.accept(storage, value);
    }

    @Override
    public T getValue(S storage) {
        return getter.apply(storage);
    }
}
