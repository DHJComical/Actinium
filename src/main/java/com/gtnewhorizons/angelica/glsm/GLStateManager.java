package com.gtnewhorizons.angelica.glsm;

import com.gtnewhorizon.gtnhlib.client.opengl.UniversalVAO;
import com.gtnewhorizons.angelica.glsm.hooks.GLSMHooks;
import com.gtnewhorizons.angelica.glsm.states.AlphaState;
import com.gtnewhorizons.angelica.glsm.states.BlendState;
import com.gtnewhorizons.angelica.glsm.states.ColorMask;
import com.gtnewhorizons.angelica.glsm.states.DepthState;
import com.gtnewhorizons.angelica.glsm.states.BooleanState;
import com.gtnewhorizons.angelica.glsm.states.FogState;
import com.gtnewhorizons.angelica.glsm.states.TextureUnitArray;
import com.gtnewhorizons.angelica.glsm.states.ViewportState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
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
import java.util.ArrayDeque;

public final class GLStateManager {
    public static final Logger LOGGER = LogManager.getLogger("Actinium/GLSM");
    public static final Capabilities capabilities = new Capabilities();

    // Stack depth constants (port from Angelica GLSM)
    public static final int MAX_ATTRIB_STACK_DEPTH = 18;
    public static final int MAX_MODELVIEW_STACK_DEPTH = 34;
    public static final int MAX_PROJECTION_STACK_DEPTH = 4;
    public static final int MAX_TEXTURE_STACK_DEPTH = 4;
    public static final int MAX_TEXTURE_UNITS = 64;

    private static final int TRACKED_TEXTURE_UNITS = MAX_TEXTURE_UNITS;

    // GPU vendor detection
    private static Vendor VENDOR = Vendor.UNKNOWN;

    // Attribute depth tracking for lazy copy-on-write
    private static int attribDepth = 0;
    private static boolean poppingAttributes = false;

    // Texture unit array for per-unit state
    public static final TextureUnitArray textures = new TextureUnitArray(MAX_TEXTURE_UNITS);

    private static final BlendState BLEND_STATE = new BlendState();
    private static final AlphaState ALPHA_STATE = new AlphaState();
    private static final BooleanState ALPHA_TEST = new BooleanState(false);
    private static final BooleanState FOG_MODE = new BooleanState(false);
    private static final FogState FOG_STATE = new FogState();
    private static final DepthState DEPTH_STATE = new DepthState();
    private static final ColorMask COLOR_MASK = new ColorMask();
    private static final Matrix4f MODEL_VIEW_MATRIX = new Matrix4f().identity();
    private static final Matrix4f PROJECTION_MATRIX = new Matrix4f().identity();
    private static final Matrix4f COLOR_MATRIX = new Matrix4f().identity();
    private static final Matrix4f[] TEXTURE_MATRICES = new Matrix4f[TRACKED_TEXTURE_UNITS];
    private static final ArrayDeque<Matrix4f> MODEL_VIEW_STACK = new ArrayDeque<>();
    private static final ArrayDeque<Matrix4f> PROJECTION_STACK = new ArrayDeque<>();
    private static final ArrayDeque<Matrix4f> COLOR_STACK = new ArrayDeque<>();
    private static final ArrayDeque<Matrix4f>[] TEXTURE_STACKS = new ArrayDeque[TRACKED_TEXTURE_UNITS];
    private static final FloatBuffer MATRIX_SCRATCH = BufferUtils.createFloatBuffer(16);
    private static int matrixMode = GL11.GL_MODELVIEW;
    private static int activeTextureUnit = 0;
    private static final int[] boundTexture2D = new int[TRACKED_TEXTURE_UNITS];
    private static int currentProgram = 0;
    private static int boundVertexArray = 0;
    private static int boundArrayBuffer = 0;
    private static int boundElementArrayBuffer = 0;
    private static int boundFramebuffer = 0;
    private static int boundReadFramebuffer = 0;
    private static int boundDrawFramebuffer = 0;
    private static final ViewportState VIEWPORT_STATE = new ViewportState();

    static {
        for (int i = 0; i < TRACKED_TEXTURE_UNITS; i++) {
            TEXTURE_MATRICES[i] = new Matrix4f().identity();
            TEXTURE_STACKS[i] = new ArrayDeque<>();
        }
    }

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

    public static Matrix4f getTextureMatrix() {
        return TEXTURE_MATRICES[trackedTextureUnit()];
    }

    public static boolean isSplashComplete() {
        return true;
    }

    public static void enableBlend() {
        if (GLSMHooks.blendHandler != null && GLSMHooks.blendHandler.isBlendLocked()) {
            GLSMHooks.blendHandler.deferBlendModeToggle(true);
            return;
        }
        GL11.glEnable(GL11.GL_BLEND);
        BLEND_STATE.setEnabled(true);
        GLSMHooks.BLEND_FUNC_CHANGE.fire(new GLSMHooks.BlendFuncChangeEvent(true, BLEND_STATE.getSrcRgb(), BLEND_STATE.getDstRgb(), BLEND_STATE.getSrcAlpha(), BLEND_STATE.getDstAlpha()));
    }

    public static void disableBlend() {
        if (GLSMHooks.blendHandler != null && GLSMHooks.blendHandler.isBlendLocked()) {
            GLSMHooks.blendHandler.deferBlendModeToggle(false);
            return;
        }
        GL11.glDisable(GL11.GL_BLEND);
        BLEND_STATE.setEnabled(false);
        GLSMHooks.BLEND_FUNC_CHANGE.fire(new GLSMHooks.BlendFuncChangeEvent(false, BLEND_STATE.getSrcRgb(), BLEND_STATE.getDstRgb(), BLEND_STATE.getSrcAlpha(), BLEND_STATE.getDstAlpha()));
    }

    public static void tryBlendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        if (GLSMHooks.blendHandler != null && GLSMHooks.blendHandler.isBlendLocked()) {
            GLSMHooks.blendHandler.deferBlendFunc(srcRgb, dstRgb, srcAlpha, dstAlpha);
            return;
        }
        GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        BLEND_STATE.setAll(srcRgb, dstRgb, srcAlpha, dstAlpha);
        GLSMHooks.BLEND_FUNC_CHANGE.fire(new GLSMHooks.BlendFuncChangeEvent(BLEND_STATE.isEnabled(), srcRgb, dstRgb, srcAlpha, dstAlpha));
    }

    public static void enableBufferBlend(int index) { GL11.glEnable(GL11.GL_BLEND); }

    public static void disableBufferBlend(int index) { GL11.glDisable(GL11.GL_BLEND); }

    public static void blendFuncSeparatei(int index, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) { GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha); }

    public static void glActiveTexture(int texture) {
        int unit = texture - GL13.GL_TEXTURE0;
        if (unit < 0) {
            unit = 0;
        }
        activeTextureUnit = unit;
        GL13.glActiveTexture(texture);
    }

    public static int getActiveTextureUnit() {
        return activeTextureUnit;
    }

    public static void glBindTexture(int target, int texture) {
        if (target == GL11.GL_TEXTURE_2D) {
            boundTexture2D[trackedTextureUnit()] = texture;
        }
        GL11.glBindTexture(target, texture);
        GLSMHooks.TEXTURE_BIND.fire(new GLSMHooks.TextureBindEvent(trackedTextureUnit(), target, texture));
    }

    public static int getBoundTextureForServerState() {
        return boundTexture2D[trackedTextureUnit()];
    }

    public static int getBoundTextureForServerState(int unit) {
        if (unit < 0 || unit >= TRACKED_TEXTURE_UNITS) {
            return 0;
        }
        return boundTexture2D[unit];
    }

    public static int glGenTextures() {
        return GL11.glGenTextures();
    }

    public static void glGenTextures(IntBuffer textures) {
        GL11.glGenTextures(textures);
    }

    public static void glDeleteTextures(int texture) {
        clearDeletedTexture(texture);
        GL11.glDeleteTextures(texture);
        GLSMHooks.TEXTURE_DELETE.fire(new GLSMHooks.TextureDeleteEvent(texture));
    }

    public static void glDeleteTextures(IntBuffer textures) {
        for (int i = textures.position(); i < textures.limit(); i++) {
            int texture = textures.get(i);
            clearDeletedTexture(texture);
            GLSMHooks.TEXTURE_DELETE.fire(new GLSMHooks.TextureDeleteEvent(texture));
        }
        GL11.glDeleteTextures(textures);
    }

    public static void glCopyImageSubData(int src, int srcTarget, int srcLevel, int srcX, int srcY, int srcZ, int dst, int dstTarget, int dstLevel, int dstX, int dstY, int dstZ, int width, int height, int depth) {
        GL43.glCopyImageSubData(src, srcTarget, srcLevel, srcX, srcY, srcZ, dst, dstTarget, dstLevel, dstX, dstY, dstZ, width, height, depth);
    }

    public static void glUseProgram(int program) {
        int previousProgram = currentProgram;
        currentProgram = program;
        GL20.glUseProgram(program);
        GLSMHooks.PROGRAM_CHANGE.fire(new GLSMHooks.ProgramChangeEvent(previousProgram, program));
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
        if (currentProgram == program) {
            currentProgram = 0;
        }
        GL20.glDeleteProgram(program);
    }

    public static int glGetAttribLocation(int program, CharSequence name) {
        return GL20.glGetAttribLocation(program, name);
    }

    public static void glBindFramebuffer(int target, int framebuffer) {
        switch (target) {
            case GL30.GL_FRAMEBUFFER -> {
                boundFramebuffer = framebuffer;
                boundReadFramebuffer = framebuffer;
                boundDrawFramebuffer = framebuffer;
            }
            case GL30.GL_READ_FRAMEBUFFER -> boundReadFramebuffer = framebuffer;
            case GL30.GL_DRAW_FRAMEBUFFER -> drawFramebufferBound(framebuffer);
            default -> {
            }
        }
        GL30.glBindFramebuffer(target, framebuffer);
    }

    public static int glCheckFramebufferStatus(int target) { return GL30.glCheckFramebufferStatus(target); }
    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) { GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level); }
    public static void glDeleteFramebuffers(int framebuffer) {
        if (boundFramebuffer == framebuffer) {
            boundFramebuffer = 0;
        }
        if (boundReadFramebuffer == framebuffer) {
            boundReadFramebuffer = 0;
        }
        if (boundDrawFramebuffer == framebuffer) {
            boundDrawFramebuffer = 0;
        }
        GL30.glDeleteFramebuffers(framebuffer);
    }

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
    public static void glBindBuffer(int target, int buffer) {
        if (target == GL15.GL_ARRAY_BUFFER) {
            boundArrayBuffer = buffer;
        } else if (target == GL15.GL_ELEMENT_ARRAY_BUFFER) {
            boundElementArrayBuffer = buffer;
        }
        GL15.glBindBuffer(target, buffer);
    }

    public static void glDeleteBuffers(int buffer) {
        if (boundArrayBuffer == buffer) {
            boundArrayBuffer = 0;
        }
        if (boundElementArrayBuffer == buffer) {
            boundElementArrayBuffer = 0;
        }
        GL15.glDeleteBuffers(buffer);
    }

    public static int glGenVertexArrays() { return UniversalVAO.genVertexArrays(); }
    public static void glBindVertexArray(int array) {
        boundVertexArray = array;
        UniversalVAO.bindVertexArray(array);
    }
    public static void glDeleteVertexArrays(int array) {
        if (boundVertexArray == array) {
            boundVertexArray = 0;
        }
        UniversalVAO.deleteVertexArrays(array);
    }
    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) { GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer); }
    public static void glEnableVertexAttribArray(int index) { GL20.glEnableVertexAttribArray(index); }
    public static void glVertexAttrib2f(int index, float x, float y) { GL20.glVertexAttrib2f(index, x, y); }
    public static void glVertexAttrib2s(int index, short x, short y) { GL20.glVertexAttrib2s(index, x, y); }
    public static void glVertexAttrib4f(int index, float x, float y, float z, float w) { GL20.glVertexAttrib4f(index, x, y, z, w); }

    public static int glGetInteger(int pname) {
        return switch (pname) {
            case GL13.GL_ACTIVE_TEXTURE -> GL13.GL_TEXTURE0 + activeTextureUnit;
            case GL11.GL_TEXTURE_BINDING_2D -> boundTexture2D[trackedTextureUnit()];
            case GL20.GL_CURRENT_PROGRAM -> currentProgram;
            case GL30.GL_VERTEX_ARRAY_BINDING -> boundVertexArray;
            case GL15.GL_ARRAY_BUFFER_BINDING -> boundArrayBuffer;
            case GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING -> boundElementArrayBuffer;
            case GL30.GL_READ_FRAMEBUFFER_BINDING -> boundReadFramebuffer;
            case GL30.GL_DRAW_FRAMEBUFFER_BINDING -> boundDrawFramebuffer;
            case GL11.GL_MATRIX_MODE -> matrixMode;
            default -> GL11.glGetInteger(pname);
        };
    }
    public static void glGetFloat(int pname, FloatBuffer params) {
        switch (pname) {
            case GL11.GL_MODELVIEW_MATRIX -> MODEL_VIEW_MATRIX.get(params);
            case GL11.GL_PROJECTION_MATRIX -> PROJECTION_MATRIX.get(params);
            case GL11.GL_TEXTURE_MATRIX -> TEXTURE_MATRICES[trackedTextureUnit()].get(params);
            default -> GL11.glGetFloatv(pname, params);
        }
    }
    public static String glGetString(int name) { return GL11.glGetString(name); }
    public static String glGetStringi(int name, int index) { return GL30.glGetStringi(name, index); }
    public static int glGetError() { return GL11.glGetError(); }
    public static int glGetTexLevelParameteri(int target, int level, int pname) { return GL11.glGetTexLevelParameteri(target, level, pname); }

    public static void glEnable(int cap) {
        if (cap == GL11.GL_BLEND) {
            enableBlend();
            return;
        } else if (cap == GL11.GL_DEPTH_TEST) {
            DEPTH_STATE.setEnabled(true);
        } else if (cap == GL11.GL_ALPHA_TEST) {
            enableAlphaTest();
            return;
        } else if (cap == GL11.GL_TEXTURE_2D) {
            textures.getTextureUnitStates(trackedTextureUnit()).setEnabled(true);
            GLSMHooks.TEXTURE_UNIT_STATE.fire(new GLSMHooks.TextureUnitStateEvent(trackedTextureUnit(), cap, true));
        }
        GL11.glEnable(cap);
    }
    public static void glDisable(int cap) {
        if (cap == GL11.GL_BLEND) {
            disableBlend();
            return;
        } else if (cap == GL11.GL_DEPTH_TEST) {
            DEPTH_STATE.setEnabled(false);
        } else if (cap == GL11.GL_ALPHA_TEST) {
            disableAlphaTest();
            return;
        } else if (cap == GL11.GL_TEXTURE_2D) {
            textures.getTextureUnitStates(trackedTextureUnit()).setEnabled(false);
            GLSMHooks.TEXTURE_UNIT_STATE.fire(new GLSMHooks.TextureUnitStateEvent(trackedTextureUnit(), cap, false));
        }
        GL11.glDisable(cap);
    }
    public static void enableDepthTest() { GL11.glEnable(GL11.GL_DEPTH_TEST); DEPTH_STATE.setEnabled(true); }
    public static void disableDepthTest() { GL11.glDisable(GL11.GL_DEPTH_TEST); DEPTH_STATE.setEnabled(false); }
    public static void enableCull() { GL11.glEnable(GL11.GL_CULL_FACE); }
    public static void disableCull() { GL11.glDisable(GL11.GL_CULL_FACE); }
    public static void enableTexture() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        textures.getTextureUnitStates(trackedTextureUnit()).setEnabled(true);
        GLSMHooks.TEXTURE_UNIT_STATE.fire(new GLSMHooks.TextureUnitStateEvent(trackedTextureUnit(), GL11.GL_TEXTURE_2D, true));
    }
    public static void enableAlphaTest() {
        if (GLSMHooks.alphaHandler != null && GLSMHooks.alphaHandler.isAlphaTestLocked()) {
            GLSMHooks.alphaHandler.deferAlphaTestToggle(true);
            return;
        }
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        ALPHA_TEST.setEnabled(true);
    }
    public static void disableAlphaTest() {
        if (GLSMHooks.alphaHandler != null && GLSMHooks.alphaHandler.isAlphaTestLocked()) {
            GLSMHooks.alphaHandler.deferAlphaTestToggle(false);
            return;
        }
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        ALPHA_TEST.setEnabled(false);
    }
    public static void glAlphaFunc(int function, float reference) {
        if (GLSMHooks.alphaHandler != null && GLSMHooks.alphaHandler.isAlphaTestLocked()) {
            GLSMHooks.alphaHandler.deferAlphaFunc(function, reference);
            return;
        }
        GL11.glAlphaFunc(function, reference);
        ALPHA_STATE.setFunction(function);
        ALPHA_STATE.setReference(reference);
    }
    public static void glDepthFunc(int function) { GL11.glDepthFunc(function); DEPTH_STATE.setFunc(function); }
    public static void glDepthMask(boolean flag) {
        if (GLSMHooks.depthColorHandler != null && GLSMHooks.depthColorHandler.isDepthColorLocked()) {
            GLSMHooks.depthColorHandler.deferDepthEnable(flag);
            return;
        }
        GL11.glDepthMask(flag);
    }
    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        if (GLSMHooks.depthColorHandler != null && GLSMHooks.depthColorHandler.isDepthColorLocked()) {
            GLSMHooks.depthColorHandler.deferColorMask(red, green, blue, alpha);
            return;
        }
        GL11.glColorMask(red, green, blue, alpha);
        COLOR_MASK.setAll(red, green, blue, alpha);
    }

    public static void glClear(int mask) { GL11.glClear(mask); }
    public static void glClearColor(float red, float green, float blue, float alpha) { GL11.glClearColor(red, green, blue, alpha); }
    public static void glViewport(int x, int y, int width, int height) {
        VIEWPORT_STATE.setViewPort(x, y, width, height);
        GL11.glViewport(x, y, width, height);
    }
    public static void glPixelStorei(int pname, int param) { GL11.glPixelStorei(pname, param); }
    public static void glTexParameteri(int target, int pname, int param) { GL11.glTexParameteri(target, pname, param); }
    public static void glCopyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height, int border) { GL11.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border); }
    public static void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) { GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height); }
    public static void glBlendColor(float red, float green, float blue, float alpha) { GL14.glBlendColor(red, green, blue, alpha); }

    public static void glDrawArrays(int mode, int first, int count) { GL11.glDrawArrays(mode, first, count); }
    public static void glMatrixMode(int mode) {
        if (mode == GL11.GL_MODELVIEW || mode == GL11.GL_PROJECTION || mode == GL11.GL_TEXTURE || mode == GL11.GL_COLOR) {
            matrixMode = mode;
        }
        GL11.glMatrixMode(mode);
    }
    public static void glPushMatrix() {
        currentMatrixStack().push(new Matrix4f(currentMatrix()));
        GL11.glPushMatrix();
    }
    public static void glPopMatrix() {
        ArrayDeque<Matrix4f> stack = currentMatrixStack();
        if (!stack.isEmpty()) {
            currentMatrix().set(stack.pop());
        }
        GL11.glPopMatrix();
    }
    public static void glLoadIdentity() {
        currentMatrix().identity();
        GL11.glLoadIdentity();
    }
    public static void glLoadMatrix(FloatBuffer matrix) {
        MATRIX_SCRATCH.clear();
        FloatBuffer copy = matrix.slice();
        int count = Math.min(copy.remaining(), 16);
        for (int i = 0; i < count; i++) {
            MATRIX_SCRATCH.put(i, copy.get(i));
        }
        currentMatrix().set(MATRIX_SCRATCH);
        GL11.glLoadMatrixf(matrix);
    }
    public static void glMultMatrix(FloatBuffer matrix) {
        MATRIX_SCRATCH.clear();
        FloatBuffer copy = matrix.slice();
        int count = Math.min(copy.remaining(), 16);
        for (int i = 0; i < count; i++) {
            MATRIX_SCRATCH.put(i, copy.get(i));
        }
        currentMatrix().mul(new Matrix4f().set(MATRIX_SCRATCH));
        GL11.glMultMatrixf(matrix);
    }
    public static void glTranslatef(float x, float y, float z) {
        currentMatrix().translate(x, y, z);
        GL11.glTranslatef(x, y, z);
    }
    public static void glScalef(float x, float y, float z) {
        currentMatrix().scale(x, y, z);
        GL11.glScalef(x, y, z);
    }
    public static void glScaled(double x, double y, double z) {
        currentMatrix().scale((float) x, (float) y, (float) z);
        GL11.glScaled(x, y, z);
    }
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

    public static int glGenFramebuffers() {
        return GL30.glGenFramebuffers();
    }

    public static ViewportState getViewportState() {
        return VIEWPORT_STATE;
    }

    private static int trackedTextureUnit() {
        if (activeTextureUnit < 0) {
            return 0;
        }
        return Math.min(activeTextureUnit, TRACKED_TEXTURE_UNITS - 1);
    }

    private static Matrix4f currentMatrix() {
        return switch (matrixMode) {
            case GL11.GL_PROJECTION -> PROJECTION_MATRIX;
            case GL11.GL_TEXTURE -> TEXTURE_MATRICES[trackedTextureUnit()];
            case GL11.GL_COLOR -> COLOR_MATRIX;
            case GL11.GL_MODELVIEW -> MODEL_VIEW_MATRIX;
            default -> MODEL_VIEW_MATRIX;
        };
    }

    private static ArrayDeque<Matrix4f> currentMatrixStack() {
        return switch (matrixMode) {
            case GL11.GL_PROJECTION -> PROJECTION_STACK;
            case GL11.GL_TEXTURE -> TEXTURE_STACKS[trackedTextureUnit()];
            case GL11.GL_COLOR -> COLOR_STACK;
            case GL11.GL_MODELVIEW -> MODEL_VIEW_STACK;
            default -> MODEL_VIEW_STACK;
        };
    }

    private static void drawFramebufferBound(int framebuffer) {
        boundDrawFramebuffer = framebuffer;
        boundFramebuffer = framebuffer;
    }

    private static void clearDeletedTexture(int texture) {
        if (texture == 0) {
            return;
        }
        for (int i = 0; i < boundTexture2D.length; i++) {
            if (boundTexture2D[i] == texture) {
                boundTexture2D[i] = 0;
            }
        }
    }

    // ==================== Vendor Detection ====================

    /**
     * Initialize GPU vendor detection. Should be called after GL context creation.
     */
    public static void detectVendor() {
        try {
            final String vendorString = GL11.glGetString(GL11.GL_VENDOR);
            VENDOR = Vendor.getVendor(vendorString);
            LOGGER.info("Detected GPU vendor: {} ({})", VENDOR, vendorString);
        } catch (Exception e) {
            VENDOR = Vendor.UNKNOWN;
            LOGGER.warn("Failed to detect GPU vendor: {}", e.getMessage());
        }
    }

    public static Vendor getVendor() {
        return VENDOR;
    }

    public static boolean vendorIsAMD() {
        return VENDOR == Vendor.AMD;
    }

    // ==================== Cache and State Helpers ====================

    /**
     * Whether to bypass the GL state cache (e.g., when splash screen is active).
     */
    public static boolean shouldBypassCache() {
        return false; // Default: always use cache
    }

    /**
     * Whether the GL state manager is currently popping attributes.
     */
    public static boolean isPoppingAttributes() {
        return poppingAttributes;
    }

    /**
     * Get the current attribute depth for lazy copy-on-write tracking.
     */
    public static int getAttribDepth() {
        return attribDepth;
    }

    /**
     * Convert integer color component to float (0-255 to 0.0-1.0).
     * Used by material/light state classes.
     */
    public static float i2f(int i) {
        return i / 255.0f;
    }

    // ==================== Capabilities ====================

    public static final class Capabilities {
        public boolean GL_ARB_copy_image = true;
        public boolean OpenGL32 = true;
    }
}
