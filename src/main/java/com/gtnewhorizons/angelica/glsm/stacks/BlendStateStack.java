package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.BlendState;

/**
 * Stack for BlendState with push/pop semantics.
 * Ported from Angelica.
 */
public class BlendStateStack extends BlendState implements IStateStack<BlendStateStack> {
    protected final BlendState[] stack;
    protected int pointer;

    public BlendStateStack() {
        stack = new BlendState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new BlendState();
        }
    }

    @Override
    public BlendStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("BlendStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public BlendStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("BlendStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
