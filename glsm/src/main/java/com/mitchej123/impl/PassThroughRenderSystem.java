package com.mitchej123.glsm.impl;

import com.mitchej123.glsm.RenderSystemService;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public abstract class PassThroughRenderSystem implements RenderSystemService {
    private static boolean compatibilityProfile;
    private final int[] shaderTextures = new int[12];
    private final float[] shaderColor = new float[] { 1f, 1f, 1f, 1f };
    private final float[] shaderFogColor = new float[4];
    private float shaderLineWidth = 1f;
    private float shaderFogStart;
    private float shaderFogEnd;
    private int fogShape;
    private Matrix4f projectionMatrix = new Matrix4f();

    public static void initializeProfileDetection() {
        compatibilityProfile = true;
    }

    public static boolean isCompatibilityProfile() {
        return compatibilityProfile;
    }

    @Override
    public void glActiveTexture(int texture) {
        GL13.glActiveTexture(texture);
    }

    @Override
    public void enableCullFace() {
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    @Override
    public void disableCullFace() {
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    @Override
    public void enableBlend() {
        GL11.glEnable(GL11.GL_BLEND);
    }

    @Override
    public void disableBlend() {
        GL11.glDisable(GL11.GL_BLEND);
    }

    @Override
    public void setUnknownBlendState() {
    }

    @Override
    public void enableDepthTest() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void disableDepthTest() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void depthFunc(int func) {
        GL11.glDepthFunc(func);
    }

    @Override
    public void depthMask(boolean flag) {
        GL11.glDepthMask(flag);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    @Override
    public void bindTexture(int texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    @Override
    public void glUniform1i(int location, int value) {
        GL20.glUniform1i(location, value);
    }

    @Override
    public void glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix3fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix4fv(location, transpose, value);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void clear(int mask, boolean checkError) {
        GL11.glClear(mask);
    }

    @Override
    public void assertOnRenderThread() {
    }

    @Override
    public void assertOnRenderThreadOrInit() {
    }

    @Override
    public void setShaderTexture(int unit, int texture) {
        shaderTextures[unit] = texture;
    }

    @Override
    public int getShaderTexture(int unit) {
        return shaderTextures[unit];
    }

    @Override
    public void setShaderColor(float red, float green, float blue, float alpha) {
        shaderColor[0] = red;
        shaderColor[1] = green;
        shaderColor[2] = blue;
        shaderColor[3] = alpha;
    }

    @Override
    public float[] getShaderColor() {
        return shaderColor.clone();
    }

    @Override
    public void setShaderFogColor(float red, float green, float blue, float alpha) {
        shaderFogColor[0] = red;
        shaderFogColor[1] = green;
        shaderFogColor[2] = blue;
        shaderFogColor[3] = alpha;
    }

    @Override
    public void setShaderFogStart(float value) {
        shaderFogStart = value;
    }

    @Override
    public void setShaderFogEnd(float value) {
        shaderFogEnd = value;
    }

    @Override
    public void setFogShape(int shape) {
        fogShape = shape;
    }

    @Override
    public float[] getShaderFogColor() {
        return shaderFogColor.clone();
    }

    @Override
    public float getShaderFogStart() {
        return shaderFogStart;
    }

    @Override
    public float getShaderFogEnd() {
        return shaderFogEnd;
    }

    @Override
    public int getFogShape() {
        return fogShape;
    }

    @Override
    public void setShaderLineWidth(float width) {
        shaderLineWidth = width;
    }

    @Override
    public float getShaderLineWidth() {
        return shaderLineWidth;
    }

    @Override
    public Matrix4f getProjectionMatrix() {
        return new Matrix4f(projectionMatrix);
    }

    @Override
    public void setProjectionMatrixOrth(Matrix4f matrix) {
        projectionMatrix = new Matrix4f(matrix);
    }

    @Override
    public void setProjectionMatrixOrigin(Matrix4f matrix) {
        projectionMatrix = new Matrix4f(matrix);
    }

    @Override
    public void defaultBlendFunc() {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }
}
