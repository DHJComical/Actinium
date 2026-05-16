package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.LineState;

/**
 * Stack for LineState with push/pop semantics.
 * Ported from Angelica.
 */
public class LineStateStack extends LineState implements IStateStack<LineStateStack> {
    protected final LineState[] stack;
    protected int pointer;

    public LineStateStack() {
        stack = new LineState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new LineState();
        }
    }

    @Override
    public LineStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("LineStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public LineStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("LineStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
