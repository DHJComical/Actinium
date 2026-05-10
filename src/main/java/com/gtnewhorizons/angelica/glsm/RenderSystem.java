package com.gtnewhorizons.angelica.glsm;

import com.gtnewhorizons.angelica.glsm.texture.TextureInfoCache;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class RenderSystem {
    private RenderSystem() {
    }

    public static boolean supportsBufferBlending() {
        return true;
    }

    public static boolean supportsImageLoadStore() {
        return true;
    }

    public static boolean supportsSamplerObjects() {
        return true;
    }

    public static boolean supportsSSBO() {
        return true;
    }

    public static boolean supportsCompute() {
        return true;
    }

    public static boolean supportsSnormFormats() {
        return true;
    }

    public static boolean supportsPackedFloatRenderable() {
        return true;
    }

    public static int getMaxGlslVersion() {
        return 460;
    }

    public static int getMaxImageUnits() {
        return 8;
    }

    public static void uniform1i(int location, int value) { GL20.glUniform1i(location, value); }
    public static void uniform1f(int location, float value) { GL20.glUniform1f(location, value); }
    public static void uniform2f(int location, float x, float y) { GL20.glUniform2f(location, x, y); }
    public static void uniform2i(int location, int x, int y) { GL20.glUniform2i(location, x, y); }
    public static void uniform3f(int location, float x, float y, float z) { GL20.glUniform3f(location, x, y, z); }
    public static void uniform4f(int location, float x, float y, float z, float w) { GL20.glUniform4f(location, x, y, z, w); }
    public static void uniform3i(int location, int x, int y, int z) { GL20.glUniform3i(location, x, y, z); }
    public static void uniform4i(int location, int x, int y, int z, int w) { GL20.glUniform4i(location, x, y, z, w); }
    public static void uniformMatrix4fv(int location, boolean transpose, java.nio.FloatBuffer buffer) { GL20.glUniformMatrix4fv(location, transpose, buffer); }

    public static int glGetUniformLocation(int program, CharSequence name) { return GL20.glGetUniformLocation(program, name); }
    public static int getUniformLocation(int program, String name) { return GL20.glGetUniformLocation(program, name); }
    public static String getActiveUniform(int program, int index, int bufSize, java.nio.IntBuffer sizeType) {
        IntBuffer size = sizeType.slice();
        size.limit(1);
        IntBuffer type = sizeType.slice();
        type.position(1);
        type.limit(2);
        return GL20.glGetActiveUniform(program, index, bufSize, size, type);
    }
    public static void bindAttributeLocation(int program, int index, CharSequence name) { GL20.glBindAttribLocation(program, index, name); }
    public static int glGetProgrami(int program, int pname) { return GL20.glGetProgrami(program, pname); }
    public static String glGetProgramInfoLog(int program, int maxLength) { return GL20.glGetProgramInfoLog(program, maxLength); }
    public static String glGetShaderInfoLog(int shader, int maxLength) { return GL20.glGetShaderInfoLog(shader, maxLength); }
    public static String getProgramInfoLog(int program) { return GL20.glGetProgramInfoLog(program, GL20.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH)); }
    public static String getShaderInfoLog(int shader) { return GL20.glGetShaderInfoLog(shader, GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH)); }

    public static int createTexture(int target) { return GL11.glGenTextures(); }
    public static void texParameteri(int texture, int target, int pname, int value) { bindTexture(texture, target); GL11.glTexParameteri(target, pname, value); }
    public static void texParameterf(int texture, int target, int pname, float value) { bindTexture(texture, target); GL11.glTexParameterf(target, pname, value); }
    public static void texParameteriv(int texture, int target, int pname, java.nio.IntBuffer values) { bindTexture(texture, target); GL11.glTexParameteriv(target, pname, values); }
    public static int getTexParameteri(int texture, int target, int pname) { bindTexture(texture, target); return GL11.glGetTexParameteri(target, pname); }
    public static void clearTexImage(int texture, int target, int level, int format, int type) { GL44.glClearTexImage(texture, level, format, type, (java.nio.ByteBuffer) null); }
    public static void copyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height, int border) {
        GL11.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border);
    }
    public static void copyTexSubImage2D(int texture, int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        bindTexture(texture, target);
        GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }
    public static void blitFramebuffer(int src, int dst, int x0, int y0, int x1, int y1, int dx0, int dy0, int dx1, int dy1, int mask, int filter) {
        GLStateManager.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src);
        GLStateManager.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst);
        GL30.glBlitFramebuffer(x0, y0, x1, y1, dx0, dy0, dx1, dy1, mask, filter);
    }
    public static void generateMipmaps(int texture, int target) { bindTexture(texture, target); GL30.glGenerateMipmap(target); }
    public static void memoryBarrier(int barriers) { GL42.glMemoryBarrier(barriers); }
    public static void bindBuffer(int target, int buffer) { GLStateManager.glBindBuffer(target, buffer); }
    public static int createBuffers() { return GL15.glGenBuffers(); }
    public static void deleteBuffers(int buffer) { GL15.glDeleteBuffers(buffer); }
    public static void bindBufferBase(int target, int index, int buffer) { GL30.glBindBufferBase(target, index, buffer); }
    public static void clearBufferSubData(int target, int internalFormat, long offset, long size, int format, int type, int[] data) {
        ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(data.length * Integer.BYTES);
        buffer.asIntBuffer().put(data);
        GL43.glClearBufferSubData(target, internalFormat, offset, size, format, type, buffer);
    }
    public static long getVRAM() { return 4L * 1024L * 1024L * 1024L; }
    public static void getProgramiv(int program, int pname, IntBuffer params) { GL20.glGetProgramiv(program, pname, params); }
    public static int bufferStorage(int target, long size, int flags) {
        int buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(target, buffer);
        GL15.glBufferData(target, size, flags);
        return buffer;
    }
    public static int bufferStorage(int target, FloatBuffer data, int flags) {
        int buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(target, buffer);
        GL15.glBufferData(target, data, flags);
        return buffer;
    }
    public static void dispatchCompute(int x, int y, int z) { GL43.glDispatchCompute(x, y, z); }
    public static void dispatchCompute(Vector3i workGroups) { GL43.glDispatchCompute(workGroups.x, workGroups.y, workGroups.z); }
    public static void dispatchComputeIndirect(long offset) { GL43.glDispatchComputeIndirect(offset); }
    public static void bindSamplerToUnit(int textureUnit, int sampler) { GL33.glBindSampler(textureUnit, sampler); }
    public static void bindTextureToUnit(int target, int unit, int texture) { GLStateManager.glBindTextureToUnit(target, unit, texture); }
    public static void destroySampler(int sampler) { GL33.glDeleteSamplers(sampler); }
    public static int genSampler() { return GL33.glGenSamplers(); }
    public static void samplerParameteri(int sampler, int pname, int param) { GL33.glSamplerParameteri(sampler, pname, param); }
    public static void bindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        GL42.glBindImageTexture(unit, texture, level, layered, layer, access, format);
    }
    public static void bindTexture(int texture, int target) { GLStateManager.glBindTexture(target, texture); }
    public static int getTexLevelParameteri(int texture, int level, int pname) { bindTexture(texture, GL11.GL_TEXTURE_2D); return GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, pname); }
    public static void texImage1D(int texture, int target, int level, int internalFormat, int width, int border, int format, int type, ByteBuffer pixels) {
        bindTexture(texture, target);
        GL11.glTexImage1D(target, level, internalFormat, width, border, format, type, pixels);
    }
    public static void texImage2D(int texture, int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
        bindTexture(texture, target);
        GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
        TextureInfoCache.INSTANCE.onTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
    }
    public static void texImage3D(int texture, int target, int level, int internalFormat, int width, int height, int depth, int border, int format, int type, ByteBuffer pixels) {
        bindTexture(texture, target);
        GL12.glTexImage3D(target, level, internalFormat, width, height, depth, border, format, type, pixels);
    }
    public static int createFramebuffer() { return GL30.glGenFramebuffers(); }
    public static void framebufferTexture2D(int fb, int fbtarget, int attachment, int target, int texture, int level) {
        GLStateManager.glBindFramebuffer(fbtarget, fb);
        GL30.glFramebufferTexture2D(fbtarget, attachment, target, texture, level);
    }
    public static void drawBuffers(int framebuffer, IntBuffer buffers) {
        GLStateManager.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL20.glDrawBuffers(buffers);
    }
    public static void detachShader(int program, int shader) { GL20.glDetachShader(program, shader); }
    public static void enableBufferBlend(int index) { GL40.glEnablei(GL11.GL_BLEND, index); }
    public static void disableBufferBlend(int index) { GL40.glDisablei(GL11.GL_BLEND, index); }
    public static void blendFuncSeparatei(int index, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        GL40.glBlendFuncSeparatei(index, srcRgb, dstRgb, srcAlpha, dstAlpha);
    }
    public static void readBuffer(int framebuffer, int buffer) {
        GLStateManager.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glReadBuffer(buffer);
    }
    public static boolean supportsTesselation() { return true; }
    private static final FloatBuffer PROJECTION_MATRIX_BUFFER = org.lwjgl.BufferUtils.createFloatBuffer(16);
    public static void setupProjectionMatrix(Matrix4f matrix) {
        GLStateManager.glMatrixMode(GL11.GL_PROJECTION);
        GLStateManager.glPushMatrix();
        PROJECTION_MATRIX_BUFFER.clear();
        matrix.get(PROJECTION_MATRIX_BUFFER);
        GLStateManager.glLoadMatrix(PROJECTION_MATRIX_BUFFER);
        GLStateManager.glMatrixMode(GL11.GL_MODELVIEW);
    }
    public static void restoreProjectionMatrix() {
        GLStateManager.glMatrixMode(GL11.GL_PROJECTION);
        GLStateManager.glPopMatrix();
        GLStateManager.glMatrixMode(GL11.GL_MODELVIEW);
    }
    public static void drawArraysIndirect(int mode, long indirect) { }
}
