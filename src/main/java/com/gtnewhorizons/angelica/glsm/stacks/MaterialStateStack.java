package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.MaterialState;

/**
 * Stack for MaterialState with push/pop semantics.
 * Ported from Angelica.
 */
public class MaterialStateStack extends MaterialState implements IStateStack<MaterialStateStack> {
    protected final MaterialState[] stack;
    protected int pointer;

    public MaterialStateStack(int face) {
        super(face);
        stack = new MaterialState[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new MaterialState(face);
        }
    }

    @Override
    public MaterialStateStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("MaterialStateStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public MaterialStateStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("MaterialStateStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
