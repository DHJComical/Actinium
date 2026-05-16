package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.FogState;

/**
 * Stack for FogState with push/pop semantics.
 * Ported from Angelica.
 */
public class FogStateStack extends FogState implements IStateStack<FogStateStack> {
    protected final FogState[] stack;
    protected int pointer;

    public FogStateStack() {
        stack = new FogState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new FogState();
        }
    }

    @Override
    public FogStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("FogStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public FogStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("FogStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
