package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.AlphaState;

/**
 * Stack for AlphaState with push/pop semantics.
 * Ported from Angelica.
 */
public class AlphaStateStack extends AlphaState implements IStateStack<AlphaStateStack> {
    protected final AlphaState[] stack;
    protected int pointer;

    public AlphaStateStack() {
        stack = new AlphaState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new AlphaState();
        }
    }

    @Override
    public AlphaStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("AlphaStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public AlphaStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("AlphaStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
