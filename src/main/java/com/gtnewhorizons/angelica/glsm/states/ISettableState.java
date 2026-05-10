package com.gtnewhorizons.angelica.glsm.states;

public interface ISettableState<T> extends Cloneable {
    T set(T state);

    boolean sameAs(Object state);

    T copy();
}
