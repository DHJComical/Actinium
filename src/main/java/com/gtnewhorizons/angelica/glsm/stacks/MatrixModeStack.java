package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.MatrixMode;

/**
 * Stack for MatrixMode with push/pop semantics.
 * Ported from Angelica.
 */
public class MatrixModeStack extends MatrixMode implements IStateStack<MatrixModeStack> {
    protected final int[] stack;
    protected int pointer;

    public MatrixModeStack() {
        stack = new int[18]; // MAX_ATTRIB_STACK_DEPTH
    }

    @Override
    public MatrixModeStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("MatrixModeStack overflow");
        }
        stack[pointer++] = mode;
        return this;
    }

    @Override
    public MatrixModeStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("MatrixModeStack underflow");
        }
        mode = stack[--pointer];
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
