package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.IntegerState;

/**
 * Stack for IntegerState with push/pop semantics.
 * Ported from Angelica.
 */
public class IntegerStateStack extends IntegerState implements IStateStack<IntegerStateStack> {
    protected final IntegerState[] stack;
    protected int pointer;

    public IntegerStateStack(int val) {
        setValue(val);
        stack = new IntegerState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new IntegerState();
        }
    }

    @Override
    public IntegerStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("IntegerStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public IntegerStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("IntegerStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
