package com.gtnewhorizons.angelica.glsm;

import com.gtnewhorizons.angelica.glsm.states.AlphaState;
import com.gtnewhorizons.angelica.glsm.states.BlendState;
import com.gtnewhorizons.angelica.glsm.states.ColorMask;
import com.gtnewhorizons.angelica.glsm.states.DepthState;
import com.gtnewhorizons.angelica.glsm.states.BooleanState;
import com.gtnewhorizons.angelica.glsm.states.FogState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class GLStateManager {
    public static final Logger LOGGER = LogManager.getLogger("Actinium/GLSM");
    public static final Capabilities capabilities = new Capabilities();

    private static final BlendState BLEND_STATE = new BlendState();
    private static final AlphaState ALPHA_STATE = new AlphaState();
    private static final BooleanState ALPHA_TEST = new BooleanState(false);
    private static final BooleanState FOG_MODE = new BooleanState(false);
    private static final FogState FOG_STATE = new FogState();
    private static final DepthState DEPTH_STATE = new DepthState();
    private static final ColorMask COLOR_MASK = new ColorMask();
    private static final Matrix4f MODEL_VIEW_MATRIX = new Matrix4f();
    private static final Matrix4f PROJECTION_MATRIX = new Matrix4f();
    private static int activeTextureUnit = 0;
    private static int boundTexture2D = 0;

    private GLStateManager() {
    }

    public static BlendState getBlendState() {
        return BLEND_STATE;
    }

    public static BlendState getBlendMode() {
        return BLEND_STATE;
    }

    public static AlphaState getAlphaState() {
        return ALPHA_STATE;
    }

    public static BooleanState getAlphaTest() {
        return ALPHA_TEST;
    }

    public static DepthState getDepthState() {
        return DEPTH_STATE;
    }

    public static ColorMask getColorMask() {
        return COLOR_MASK;
    }

    public static Matrix4f getModelViewMatrix() {
        return MODEL_VIEW_MATRIX;
    }

    public static Matrix4f getProjectionMatrix() {
        return PROJECTION_MATRIX;
    }

    public static boolean isSplashComplete() {
        return true;
    }

    public static void enableBlend() {
        GL11.glEnable(GL11.GL_BLEND);
        BLEND_STATE.setEnabled(true);
    }

    public static void disableBlend() {
        GL11.glDisable(GL11.GL_BLEND);
        BLEND_STATE.setEnabled(false);
    }

    public static void tryBlendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        BLEND_STATE.setAll(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }

    public static void enableBufferBlend(int index) { GL11.glEnable(GL11.GL_BLEND); }

    public static void disableBufferBlend(int index) { GL11.glDisable(GL11.GL_BLEND); }

    public static void blendFuncSeparatei(int index, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) { GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha); }

    public static void glActiveTexture(int texture) {
        activeTextureUnit = texture - GL13.GL_TEXTURE0;
        GL13.glActiveTexture(texture);
    }

    public static int getActiveTextureUnit() {
        return activeTextureUnit;
    }

    public static void glBindTexture(int target, int texture) {
        if (target == GL11.GL_TEXTURE_2D) {
            boundTexture2D = texture;
        }
        GL11.glBindTexture(target, texture);
    }

    public static int getBoundTextureForServerState() {
        return boundTexture2D;
    }

    public static int getBoundTextureForServerState(int unit) {
        return boundTexture2D;
    }

    public static int glGenTextures() {
        return GL11.glGenTextures();
    }

    public static void glGenTextures(IntBuffer textures) {
        GL11.glGenTextures(textures);
    }

    public static void glDeleteTextures(int texture) {
        GL11.glDeleteTextures(texture);
    }

    public static void glDeleteTextures(IntBuffer textures) {
        GL11.glDeleteTextures(textures);
    }

    public static void glCopyImageSubData(int src, int srcTarget, int srcLevel, int srcX, int srcY, int srcZ, int dst, int dstTarget, int dstLevel, int dstX, int dstY, int dstZ, int width, int height, int depth) {
        GL43.glCopyImageSubData(src, srcTarget, srcLevel, srcX, srcY, srcZ, dst, dstTarget, dstLevel, dstX, dstY, dstZ, width, height, depth);
    }

    public static void glUseProgram(int program) {
        GL20.glUseProgram(program);
    }

    public static int glCreateProgram() { return GL20.glCreateProgram(); }
    public static int glCreateShader(int type) { return GL20.glCreateShader(type); }
    public static void glShaderSource(int shader, CharSequence source) { GL20.glShaderSource(shader, source); }
    public static void glCompileShader(int shader) { GL20.glCompileShader(shader); }
    public static int glGetShaderi(int shader, int pname) { return GL20.glGetShaderi(shader, pname); }
    public static String glGetShaderInfoLog(int shader, int maxLength) { return GL20.glGetShaderInfoLog(shader, maxLength); }
    public static void glAttachShader(int program, int shader) { GL20.glAttachShader(program, shader); }
    public static void glDeleteShader(int shader) { GL20.glDeleteShader(shader); }
    public static void glBindAttribLocation(int program, int index, CharSequence name) { GL20.glBindAttribLocation(program, index, name); }
    public static void glLinkProgram(int program) { GL20.glLinkProgram(program); }
    public static int glGetProgrami(int program, int pname) { return GL20.glGetProgrami(program, pname); }
    public static String glGetProgramInfoLog(int program, int maxLength) { return GL20.glGetProgramInfoLog(program, maxLength); }
    public static int glGetUniformLocation(int program, CharSequence name) { return GL20.glGetUniformLocation(program, name); }

    public static void glDeleteProgram(int program) {
        GL20.glDeleteProgram(program);
    }

    public static void glBindFramebuffer(int target, int framebuffer) {
        GL30.glBindFramebuffer(target, framebuffer);
    }

    public static int glCheckFramebufferStatus(int target) { return GL30.glCheckFramebufferStatus(target); }
    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) { GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level); }
    public static void glDeleteFramebuffers(int framebuffer) { GL30.glDeleteFramebuffers(framebuffer); }

    public static void glDrawBuffers(int[] buffers) {
        GL20.glDrawBuffers(buffers);
    }

    public static void glBindTextureToUnit(int target, int unit, int texture) {
        glActiveTexture(GL13.GL_TEXTURE0 + unit);
        glBindTexture(target, texture);
    }

    public static void bindTextureToUnit(int target, int unit, int texture) {
        glBindTextureToUnit(target, unit, texture);
    }

    public static void glUniform1i(int location, int value) { GL20.glUniform1i(location, value); }
    public static void glUniform1f(int location, float value) { GL20.glUniform1f(location, value); }
    public static void glUniform2f(int location, float x, float y) { GL20.glUniform2f(location, x, y); }
    public static void glUniform2i(int location, int x, int y) { GL20.glUniform2i(location, x, y); }
    public static void glUniform3f(int location, float x, float y, float z) { GL20.glUniform3f(location, x, y, z); }
    public static void glUniform3i(int location, int x, int y, int z) { GL20.glUniform3i(location, x, y, z); }
    public static void glUniform4f(int location, float x, float y, float z, float w) { GL20.glUniform4f(location, x, y, z, w); }
    public static void glUniformMatrix3(int location, boolean transpose, FloatBuffer buffer) { GL20.glUniformMatrix3fv(location, transpose, buffer); }
    public static void glUniformMatrix4(int location, boolean transpose, FloatBuffer buffer) { GL20.glUniformMatrix4fv(location, transpose, buffer); }

    public static int glGenBuffers() { return GL15.glGenBuffers(); }
    public static void glBindBuffer(int target, int buffer) { GL15.glBindBuffer(target, buffer); }

    public static int glGenVertexArrays() { return GL30.glGenVertexArrays(); }
    public static void glBindVertexArray(int array) { GL30.glBindVertexArray(array); }
    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) { GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer); }
    public static void glEnableVertexAttribArray(int index) { GL20.glEnableVertexAttribArray(index); }
    public static void glVertexAttrib2f(int index, float x, float y) { GL20.glVertexAttrib2f(index, x, y); }
    public static void glVertexAttrib2s(int index, short x, short y) { GL20.glVertexAttrib2s(index, x, y); }
    public static void glVertexAttrib4f(int index, float x, float y, float z, float w) { GL20.glVertexAttrib4f(index, x, y, z, w); }

    public static int glGetInteger(int pname) { return GL11.glGetInteger(pname); }
    public static void glGetFloat(int pname, FloatBuffer params) { GL11.glGetFloatv(pname, params); }
    public static String glGetString(int name) { return GL11.glGetString(name); }
    public static String glGetStringi(int name, int index) { return GL30.glGetStringi(name, index); }
    public static int glGetError() { return GL11.glGetError(); }
    public static int glGetTexLevelParameteri(int target, int level, int pname) { return GL11.glGetTexLevelParameteri(target, level, pname); }

    public static void glEnable(int cap) { GL11.glEnable(cap); }
    public static void glDisable(int cap) { GL11.glDisable(cap); }
    public static void enableDepthTest() { GL11.glEnable(GL11.GL_DEPTH_TEST); DEPTH_STATE.setEnabled(true); }
    public static void disableDepthTest() { GL11.glDisable(GL11.GL_DEPTH_TEST); DEPTH_STATE.setEnabled(false); }
    public static void enableCull() { GL11.glEnable(GL11.GL_CULL_FACE); }
    public static void disableCull() { GL11.glDisable(GL11.GL_CULL_FACE); }
    public static void enableTexture() { GL11.glEnable(GL11.GL_TEXTURE_2D); }
    public static void enableAlphaTest() { GL11.glEnable(GL11.GL_ALPHA_TEST); ALPHA_TEST.setEnabled(true); }
    public static void disableAlphaTest() { GL11.glDisable(GL11.GL_ALPHA_TEST); ALPHA_TEST.setEnabled(false); }
    public static void glAlphaFunc(int function, float reference) { GL11.glAlphaFunc(function, reference); ALPHA_STATE.setFunction(function); ALPHA_STATE.setReference(reference); }
    public static void glDepthFunc(int function) { GL11.glDepthFunc(function); DEPTH_STATE.setFunc(function); }
    public static void glDepthMask(boolean flag) { GL11.glDepthMask(flag); DEPTH_STATE.setEnabled(flag); }
    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) { GL11.glColorMask(red, green, blue, alpha); COLOR_MASK.setAll(red, green, blue, alpha); }

    public static void glClear(int mask) { GL11.glClear(mask); }
    public static void glClearColor(float red, float green, float blue, float alpha) { GL11.glClearColor(red, green, blue, alpha); }
    public static void glViewport(int x, int y, int width, int height) { GL11.glViewport(x, y, width, height); }
    public static void glPixelStorei(int pname, int param) { GL11.glPixelStorei(pname, param); }
    public static void glTexParameteri(int target, int pname, int param) { GL11.glTexParameteri(target, pname, param); }
    public static void glCopyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height, int border) { GL11.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border); }
    public static void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) { GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height); }
    public static void glBlendColor(float red, float green, float blue, float alpha) { GL14.glBlendColor(red, green, blue, alpha); }

    public static void glDrawArrays(int mode, int first, int count) { GL11.glDrawArrays(mode, first, count); }
    public static void glMatrixMode(int mode) { GL11.glMatrixMode(mode); }
    public static void glPushMatrix() { GL11.glPushMatrix(); }
    public static void glPopMatrix() { GL11.glPopMatrix(); }
    public static void glLoadIdentity() { GL11.glLoadIdentity(); }
    public static void glLoadMatrix(FloatBuffer matrix) { GL11.glLoadMatrixf(matrix); }
    public static void glMultMatrix(FloatBuffer matrix) { GL11.glMultMatrixf(matrix); }
    public static void glTranslatef(float x, float y, float z) { GL11.glTranslatef(x, y, z); }
    public static void glScalef(float x, float y, float z) { GL11.glScalef(x, y, z); }
    public static void glScaled(double x, double y, double z) { GL11.glScaled(x, y, z); }
    public static void glColor4f(float red, float green, float blue, float alpha) { GL11.glColor4f(red, green, blue, alpha); }
    public static void glShadeModel(int mode) { GL11.glShadeModel(mode); }
    public static void glPushAttrib(int mask) { GL11.glPushAttrib(mask); }
    public static void glPopAttrib() { GL11.glPopAttrib(); }
    public static void glPolygonOffset(float factor, float units) { GL11.glPolygonOffset(factor, units); }

    public static void defaultBlendFunc() { tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO); }
    public static BooleanState getFogMode() { return FOG_MODE; }
    public static FogState getFogState() { return FOG_STATE; }
    public static org.joml.Vector3d getFogColor() { return new org.joml.Vector3d(0.0D, 0.0D, 0.0D); }
    public static void setLightmapTextureCoords(int target, float x, float y) { GL13.glMultiTexCoord2f(target, x, y); }
    public static void glGetTexImage(int target, int level, int format, int type, IntBuffer pixels) { GL11.glGetTexImage(target, level, format, type, pixels); }

    public static final class Capabilities {
        public boolean GL_ARB_copy_image = true;
        public boolean OpenGL32 = true;
    }
}
