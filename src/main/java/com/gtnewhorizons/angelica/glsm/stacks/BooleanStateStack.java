package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.states.BooleanState;

/**
 * Stack for BooleanState with push/pop semantics and lazy copy-on-write.
 * Ported from Angelica.
 */
public class BooleanStateStack extends BooleanState implements IStateStack<BooleanStateStack> {
    protected final int glCap;
    protected final boolean ffpStateOnly;
    protected final boolean[] stack;
    protected int savedDepth;

    public BooleanStateStack(int glCap) {
        this(glCap, false);
    }

    public BooleanStateStack(int glCap, boolean initialState) {
        this(glCap, initialState, false);
    }

    public BooleanStateStack(int glCap, boolean initialState, boolean ffpStateOnly) {
        super(initialState);
        this.glCap = glCap;
        this.ffpStateOnly = ffpStateOnly;
        this.enabled = initialState;
        stack = new boolean[18]; // MAX_ATTRIB_STACK_DEPTH
    }

    @Override
    public BooleanStateStack push() {
        if (savedDepth >= stack.length) {
            throw new IllegalStateException("BooleanStateStack overflow");
        }
        stack[savedDepth++] = enabled;
        return this;
    }

    @Override
    public BooleanStateStack pop() {
        if (savedDepth == 0) {
            throw new IllegalStateException("BooleanStateStack underflow");
        }
        final boolean oldValue = stack[--savedDepth];
        setEnabled(oldValue);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return savedDepth == 0;
    }
}
