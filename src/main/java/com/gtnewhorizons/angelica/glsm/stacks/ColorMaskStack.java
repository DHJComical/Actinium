package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.ColorMask;

/**
 * Stack for ColorMask with push/pop semantics.
 * Ported from Angelica.
 */
public class ColorMaskStack extends ColorMask implements IStateStack<ColorMaskStack> {
    protected final ColorMask[] stack;
    protected int pointer;

    public ColorMaskStack() {
        stack = new ColorMask[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new ColorMask();
        }
    }

    @Override
    public ColorMaskStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("ColorMaskStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public ColorMaskStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("ColorMaskStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
