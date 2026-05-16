package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.PolygonState;

/**
 * Stack for PolygonState with push/pop semantics.
 * Ported from Angelica.
 */
public class PolygonStateStack extends PolygonState implements IStateStack<PolygonStateStack> {
    protected final PolygonState[] stack;
    protected int pointer;

    public PolygonStateStack() {
        stack = new PolygonState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new PolygonState();
        }
    }

    @Override
    public PolygonStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("PolygonStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public PolygonStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("PolygonStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
