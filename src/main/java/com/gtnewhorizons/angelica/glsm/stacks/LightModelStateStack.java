package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.LightModelState;

/**
 * Stack for LightModelState with push/pop semantics.
 * Ported from Angelica.
 */
public class LightModelStateStack extends LightModelState implements IStateStack<LightModelStateStack> {
    protected final LightModelState[] stack;
    protected int pointer;

    public LightModelStateStack() {
        super();
        stack = new LightModelState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new LightModelState();
        }
    }

    @Override
    public LightModelStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("LightModelStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public LightModelStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("LightModelStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
