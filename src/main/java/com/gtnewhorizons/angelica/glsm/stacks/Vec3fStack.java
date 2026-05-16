package com.gtnewhorizons.angelica.glsm.stacks;

import org.joml.Vector3f;

/**
 * Stack for a Vector3f value (e.g. current normal).
 * Ported from Angelica.
 */
public class Vec3fStack implements IStateStack<Vec3fStack> {
    private final Vector3f value;
    private final float[][] stack;
    private int pointer;

    public Vec3fStack(Vector3f value) {
        this.value = value;
        stack = new float[18][3]; // MAX_ATTRIB_STACK_DEPTH
    }

    @Override
    public Vec3fStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("Vec3fStack overflow");
        }
        stack[pointer][0] = value.x;
        stack[pointer][1] = value.y;
        stack[pointer][2] = value.z;
        pointer++;
        return this;
    }

    @Override
    public Vec3fStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("Vec3fStack underflow");
        }
        pointer--;
        value.set(stack[pointer][0], stack[pointer][1], stack[pointer][2]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
