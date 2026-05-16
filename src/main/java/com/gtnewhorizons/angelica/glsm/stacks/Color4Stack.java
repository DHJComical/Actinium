package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.Color4;

/**
 * Stack for Color4 with push/pop semantics.
 * Ported from Angelica.
 */
public class Color4Stack extends Color4 implements IStateStack<Color4Stack> {
    protected final Color4[] stack;
    protected int pointer;

    public Color4Stack() {
        stack = new Color4[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new Color4();
        }
    }

    public Color4Stack(Color4 color4) {
        this();
        set(color4);
    }

    @Override
    public Color4Stack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("Color4Stack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public Color4Stack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("Color4Stack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
