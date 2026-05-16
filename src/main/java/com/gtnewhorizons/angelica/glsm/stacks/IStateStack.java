package com.gtnewhorizons.angelica.glsm.stacks;

/**
 * Interface for state stacks used in GL state management.
 * Provides push/pop semantics for GL attribute stacks.
 */
public interface IStateStack<T> {
    T push();
    T pop();
    boolean isEmpty();

    default int pushDepth() {
        return 0;
    }

    default T popDepth() {
        return pop();
    }
}
