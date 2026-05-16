package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.TextureBinding;

/**
 * Stack for TextureBinding with push/pop semantics.
 * Ported from Angelica.
 */
public class TextureBindingStack extends TextureBinding implements IStateStack<TextureBindingStack> {
    protected final TextureBinding[] stack;
    protected int pointer;

    public TextureBindingStack() {
        stack = new TextureBinding[18]; // MAX_ATTRIB_STACK_DEPTH
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new TextureBinding();
        }
    }

    @Override
    public TextureBindingStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("TextureBindingStack overflow");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public TextureBindingStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("TextureBindingStack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    public TextureBinding peek() {
        return stack[pointer];
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
