package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.DepthState;

/**
 * Stack for DepthState with push/pop semantics.
 * Ported from Angelica.
 */
public class DepthStateStack extends DepthState implements IStateStack<DepthStateStack> {
    protected final DepthState[] stack;
    protected int pointer;

    public DepthStateStack() {
        stack = new DepthState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new DepthState();
        }
    }

    @Override
    public DepthStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("DepthStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public DepthStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("DepthStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
