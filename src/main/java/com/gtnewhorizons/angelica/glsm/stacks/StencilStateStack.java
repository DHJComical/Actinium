package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.StencilState;

/**
 * Stack for StencilState with push/pop semantics.
 * Ported from Angelica.
 */
public class StencilStateStack extends StencilState implements IStateStack<StencilStateStack> {
    protected final StencilState[] stack;
    protected int pointer;

    public StencilStateStack() {
        stack = new StencilState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new StencilState();
        }
    }

    @Override
    public StencilStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("StencilStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public StencilStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("StencilStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
