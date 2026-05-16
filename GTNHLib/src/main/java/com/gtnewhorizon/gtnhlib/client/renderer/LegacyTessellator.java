package com.gtnewhorizon.gtnhlib.client.renderer;

import java.util.Arrays;

import org.lwjgl.opengl.GL11;

/**
 * Minimal 1.7-style tessellator compatibility layer used by the GTNHLib port.
 *
 * <p>This keeps the old field and method surface that the migrated GTNHLib code expects,
 * without depending on 1.12's vanilla Tessellator internals.</p>
 */
public class LegacyTessellator {

    protected int[] rawBuffer;
    protected int rawBufferIndex;
    protected int rawBufferSize;
    protected boolean isDrawing;
    protected boolean hasTexture;
    protected boolean hasBrightness;
    protected boolean hasColor;
    protected boolean hasNormals;
    protected boolean isColorDisabled;
    protected int vertexCount;
    protected int drawMode = GL11.GL_QUADS;
    protected double xOffset;
    protected double yOffset;
    protected double zOffset;

    public void startDrawing(int drawMode) {
        if (this.isDrawing) {
            throw new IllegalStateException("Already tessellating");
        }

        reset();
        this.isDrawing = true;
        this.drawMode = drawMode;
    }

    public void startDrawingQuads() {
        startDrawing(GL11.GL_QUADS);
    }

    public int draw() {
        final int result = this.rawBufferIndex * Integer.BYTES;
        discard();
        return result;
    }

    public void reset() {
        this.rawBufferIndex = 0;
        this.vertexCount = 0;
        this.hasTexture = false;
        this.hasBrightness = false;
        this.hasColor = false;
        this.hasNormals = false;
        this.isColorDisabled = false;
        this.drawMode = GL11.GL_QUADS;
    }

    public void discard() {
        this.isDrawing = false;
        reset();
    }

    public void setTranslation(double x, double y, double z) {
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;
    }

    public void addTranslation(float x, float y, float z) {
        this.xOffset += x;
        this.yOffset += y;
        this.zOffset += z;
    }

    public void setNormal(float x, float y, float z) {
        // Overridden by implementations that actually store per-vertex normal state.
    }

    public void disableColor() {
        this.isColorDisabled = true;
    }

    protected void ensureBuffer(int intsNeeded) {
        final int required = this.rawBufferIndex + intsNeeded;
        if (this.rawBuffer == null) {
            this.rawBufferSize = Math.max(0x10000, required);
            this.rawBuffer = new int[this.rawBufferSize];
            return;
        }

        if (required <= this.rawBufferSize) {
            return;
        }

        int newSize = this.rawBufferSize;
        while (newSize < required) {
            newSize *= 2;
        }
        this.rawBuffer = Arrays.copyOf(this.rawBuffer, newSize);
        this.rawBufferSize = newSize;
    }
}
