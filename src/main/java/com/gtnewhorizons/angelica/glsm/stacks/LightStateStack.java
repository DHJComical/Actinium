package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.LightState;

/**
 * Stack for LightState with push/pop semantics.
 * Ported from Angelica.
 */
public class LightStateStack extends LightState implements IStateStack<LightStateStack> {
    protected final LightState[] stack;
    protected int pointer;

    public LightStateStack(int light) {
        super(light);
        stack = new LightState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new LightState(light);
        }
    }

    @Override
    public LightStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("LightStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public LightStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("LightStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
