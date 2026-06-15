package com.mitchej123.glsm;

import org.joml.Matrix4f;

import java.nio.FloatBuffer;

public interface RenderSystemService {
    default int getPriority() {
        return 0;
    }

    void glActiveTexture(int texture);
    void bindTexture(int texture);
    void enableCullFace();
    void disableCullFace();
    void enableBlend();
    void disableBlend();
    void defaultBlendFunc();
    void blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha);
    void setUnknownBlendState();
    void enableDepthTest();
    void disableDepthTest();
    void depthFunc(int func);
    void depthMask(boolean flag);
    void glViewport(int x, int y, int width, int height);
    void glClearColor(float red, float green, float blue, float alpha);
    void clear(int mask, boolean checkError);
    void glUniform1i(int location, int value);
    void glUniformMatrix3(int location, boolean transpose, FloatBuffer value);
    void glUniformMatrix4(int location, boolean transpose, FloatBuffer value);
    void assertOnRenderThread();
    void assertOnRenderThreadOrInit();
    void setShaderTexture(int unit, int texture);
    int getShaderTexture(int unit);
    void setShaderColor(float red, float green, float blue, float alpha);
    float[] getShaderColor();
    void setShaderLineWidth(float width);
    float getShaderLineWidth();
    void setShaderFogColor(float red, float green, float blue, float alpha);
    float[] getShaderFogColor();
    void setShaderFogStart(float value);
    float getShaderFogStart();
    void setShaderFogEnd(float value);
    float getShaderFogEnd();
    void setFogShape(int shape);
    int getFogShape();
    Matrix4f getProjectionMatrix();
    void setProjectionMatrixOrth(Matrix4f matrix);
    void setProjectionMatrixOrigin(Matrix4f matrix);

    default void setPositionShader() {
    }
}
