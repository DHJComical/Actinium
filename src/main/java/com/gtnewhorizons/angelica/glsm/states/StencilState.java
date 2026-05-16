package com.gtnewhorizons.angelica.glsm.states;

import org.lwjgl.opengl.GL11;

/**
 * Tracks GL stencil buffer state with separate front/back support.
 * Ported from Angelica.
 */
public class StencilState implements ISettableState<StencilState> {
    // Front face
    private int funcFront = GL11.GL_ALWAYS;
    private int refFront = 0;
    private int valueMaskFront = 0xFFFFFFFF;
    private int failOpFront = GL11.GL_KEEP;
    private int zFailOpFront = GL11.GL_KEEP;
    private int zPassOpFront = GL11.GL_KEEP;
    private int writeMaskFront = 0xFFFFFFFF;

    // Back face
    private int funcBack = GL11.GL_ALWAYS;
    private int refBack = 0;
    private int valueMaskBack = 0xFFFFFFFF;
    private int failOpBack = GL11.GL_KEEP;
    private int zFailOpBack = GL11.GL_KEEP;
    private int zPassOpBack = GL11.GL_KEEP;
    private int writeMaskBack = 0xFFFFFFFF;

    private int clearValue = 0;

    // Getters
    public int getFuncFront() { return funcFront; }
    public void setFuncFront(int v) { this.funcFront = v; }
    public int getRefFront() { return refFront; }
    public void setRefFront(int v) { this.refFront = v; }
    public int getValueMaskFront() { return valueMaskFront; }
    public void setValueMaskFront(int v) { this.valueMaskFront = v; }
    public int getFailOpFront() { return failOpFront; }
    public void setFailOpFront(int v) { this.failOpFront = v; }
    public int getZFailOpFront() { return zFailOpFront; }
    public void setZFailOpFront(int v) { this.zFailOpFront = v; }
    public int getZPassOpFront() { return zPassOpFront; }
    public void setZPassOpFront(int v) { this.zPassOpFront = v; }
    public int getWriteMaskFront() { return writeMaskFront; }
    public void setWriteMaskFront(int v) { this.writeMaskFront = v; }

    public int getFuncBack() { return funcBack; }
    public void setFuncBack(int v) { this.funcBack = v; }
    public int getRefBack() { return refBack; }
    public void setRefBack(int v) { this.refBack = v; }
    public int getValueMaskBack() { return valueMaskBack; }
    public void setValueMaskBack(int v) { this.valueMaskBack = v; }
    public int getFailOpBack() { return failOpBack; }
    public void setFailOpBack(int v) { this.failOpBack = v; }
    public int getZFailOpBack() { return zFailOpBack; }
    public void setZFailOpBack(int v) { this.zFailOpBack = v; }
    public int getZPassOpBack() { return zPassOpBack; }
    public void setZPassOpBack(int v) { this.zPassOpBack = v; }
    public int getWriteMaskBack() { return writeMaskBack; }
    public void setWriteMaskBack(int v) { this.writeMaskBack = v; }
    public int getClearValue() { return clearValue; }
    public void setClearValue(int v) { this.clearValue = v; }

    public void setFunc(int func, int ref, int mask) {
        this.funcFront = func;
        this.refFront = ref;
        this.valueMaskFront = mask;
        this.funcBack = func;
        this.refBack = ref;
        this.valueMaskBack = mask;
    }

    public void setOp(int fail, int zFail, int zPass) {
        this.failOpFront = fail;
        this.zFailOpFront = zFail;
        this.zPassOpFront = zPass;
        this.failOpBack = fail;
        this.zFailOpBack = zFail;
        this.zPassOpBack = zPass;
    }

    public void setWriteMask(int mask) {
        this.writeMaskFront = mask;
        this.writeMaskBack = mask;
    }

    @Override
    public StencilState set(StencilState state) {
        this.funcFront = state.funcFront;
        this.refFront = state.refFront;
        this.valueMaskFront = state.valueMaskFront;
        this.failOpFront = state.failOpFront;
        this.zFailOpFront = state.zFailOpFront;
        this.zPassOpFront = state.zPassOpFront;
        this.writeMaskFront = state.writeMaskFront;
        this.funcBack = state.funcBack;
        this.refBack = state.refBack;
        this.valueMaskBack = state.valueMaskBack;
        this.failOpBack = state.failOpBack;
        this.zFailOpBack = state.zFailOpBack;
        this.zPassOpBack = state.zPassOpBack;
        this.writeMaskBack = state.writeMaskBack;
        this.clearValue = state.clearValue;
        return this;
    }

    @Override
    public boolean sameAs(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StencilState ss)) return false;
        return funcFront == ss.funcFront && refFront == ss.refFront && valueMaskFront == ss.valueMaskFront
            && failOpFront == ss.failOpFront && zFailOpFront == ss.zFailOpFront && zPassOpFront == ss.zPassOpFront
            && writeMaskFront == ss.writeMaskFront
            && funcBack == ss.funcBack && refBack == ss.refBack && valueMaskBack == ss.valueMaskBack
            && failOpBack == ss.failOpBack && zFailOpBack == ss.zFailOpBack && zPassOpBack == ss.zPassOpBack
            && writeMaskBack == ss.writeMaskBack && clearValue == ss.clearValue;
    }

    @Override
    public StencilState copy() {
        return new StencilState().set(this);
    }
}
