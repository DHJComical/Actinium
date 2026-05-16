package com.gtnewhorizons.angelica.glsm.states;

import com.gtnewhorizons.angelica.glsm.stacks.IStateStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Per-texture-unit GL_TEXTURE_ENV state for FFP emulation.
 * Ported from Angelica.
 */
public class TexEnvState implements IStateStack<TexEnvState> {
    public int mode = GL11.GL_MODULATE;
    public int combineRgb = GL11.GL_MODULATE;
    public int combineAlpha = GL11.GL_MODULATE;

    public final int[] sourceRgb = new int[3];
    public final int[] sourceAlpha = new int[3];
    public final int[] operandRgb = new int[3];
    public final int[] operandAlpha = new int[3];

    public float scaleRgb = 1.0f;
    public float scaleAlpha = 1.0f;

    public float envColorR = 0.0f;
    public float envColorG = 0.0f;
    public float envColorB = 0.0f;
    public float envColorA = 0.0f;

    private final TexEnvState[] stack;
    private int pointer;

    public TexEnvState() {
        stack = new TexEnvState[18]; // MAX_ATTRIB_STACK_DEPTH default
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new TexEnvState(true);
        }
        reset();
    }

    private TexEnvState(boolean isStackEntry) {
        this.stack = null;
        if (!isStackEntry) {
            throw new IllegalArgumentException("Use TexEnvState() constructor");
        }
        reset();
    }

    public void copyFrom(TexEnvState other) {
        this.mode = other.mode;
        this.combineRgb = other.combineRgb;
        this.combineAlpha = other.combineAlpha;
        System.arraycopy(other.sourceRgb, 0, this.sourceRgb, 0, 3);
        System.arraycopy(other.sourceAlpha, 0, this.sourceAlpha, 0, 3);
        System.arraycopy(other.operandRgb, 0, this.operandRgb, 0, 3);
        System.arraycopy(other.operandAlpha, 0, this.operandAlpha, 0, 3);
        this.scaleRgb = other.scaleRgb;
        this.scaleAlpha = other.scaleAlpha;
        this.envColorR = other.envColorR;
        this.envColorG = other.envColorG;
        this.envColorB = other.envColorB;
        this.envColorA = other.envColorA;
    }

    public void reset() {
        mode = GL11.GL_MODULATE;
        combineRgb = GL11.GL_MODULATE;
        combineAlpha = GL11.GL_MODULATE;
        sourceRgb[0] = GL11.GL_TEXTURE; sourceRgb[1] = GL13.GL_PREVIOUS; sourceRgb[2] = GL13.GL_CONSTANT;
        sourceAlpha[0] = GL11.GL_TEXTURE; sourceAlpha[1] = GL13.GL_PREVIOUS; sourceAlpha[2] = GL13.GL_CONSTANT;
        operandRgb[0] = GL11.GL_SRC_COLOR; operandRgb[1] = GL11.GL_SRC_COLOR; operandRgb[2] = GL11.GL_SRC_ALPHA;
        operandAlpha[0] = GL11.GL_SRC_ALPHA; operandAlpha[1] = GL11.GL_SRC_ALPHA; operandAlpha[2] = GL11.GL_SRC_ALPHA;
        scaleRgb = 1.0f;
        scaleAlpha = 1.0f;
        envColorR = 0.0f; envColorG = 0.0f; envColorB = 0.0f; envColorA = 0.0f;
    }

    @Override
    public TexEnvState push() {
        if (pointer >= stack.length) {
            throw new IllegalStateException("TexEnvState stack overflow");
        }
        stack[pointer++].copyFrom(this);
        return this;
    }

    @Override
    public TexEnvState pop() {
        if (pointer == 0) {
            throw new IllegalStateException("TexEnvState stack underflow");
        }
        copyFrom(stack[--pointer]);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return pointer == 0;
    }
}
