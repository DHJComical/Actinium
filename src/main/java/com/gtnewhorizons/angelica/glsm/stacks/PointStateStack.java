package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.PointState;

/**
 * Stack for PointState with push/pop semantics.
 * Ported from Angelica.
 */
public class PointStateStack extends PointState implements IStateStack<PointStateStack> {
    protected final PointState[] stack;
    protected int pointer;

    public PointStateStack() {
        stack = new PointState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new PointState();
        }
    }

    @Override
    public PointStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("PointStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public PointStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("PointStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
