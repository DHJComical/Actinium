package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.ViewportState;

/**
 * Stack for ViewportState with push/pop semantics.
 * Ported from Angelica.
 */
public class ViewPortStateStack extends ViewportState implements IStateStack<ViewPortStateStack> {
    protected final ViewportState[] stack;
    protected int pointer;

    public ViewPortStateStack() {
        stack = new ViewportState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new ViewportState();
        }
    }

    @Override
    public ViewPortStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("ViewPortStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public ViewPortStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("ViewPortStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
