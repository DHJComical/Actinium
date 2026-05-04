package com.dhj.actinium.shader.pipeline;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

final class ActiniumGlStateGuard implements AutoCloseable {
    private static final IntBuffer SCRATCH_COLOR_MASK_BUFFER = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer SCRATCH_ALPHA_REF_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer SCRATCH_LINE_WIDTH_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer SCRATCH_CURRENT_COLOR_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer SCRATCH_POLYGON_MODE_BUFFER = BufferUtils.createIntBuffer(16);
    private static final IntBuffer SCRATCH_SCISSOR_BOX_BUFFER = BufferUtils.createIntBuffer(16);
    private static final IntBuffer SCRATCH_VIEWPORT_BUFFER = BufferUtils.createIntBuffer(16);

    private final int framebufferBinding;
    private final int drawBuffer;
    private final int readBuffer;
    private final int activeTexture;
    private final int textureBinding2d;
    private final int[] textureBindings2d;
    private final int currentProgram;
    private final int vertexArrayBinding;
    private final int arrayBufferBinding;
    private final int elementArrayBufferBinding;
    private final int matrixMode;
    private final int shadeModel;
    private final int polygonModeFront;
    private final int polygonModeBack;
    private final float lineWidth;
    private final int depthFunc;
    private final int blendSrcRgb;
    private final int blendDstRgb;
    private final int blendSrcAlpha;
    private final int blendDstAlpha;
    private final int cullFaceMode;
    private final int frontFace;
    private final boolean blendEnabled;
    private final boolean cullEnabled;
    private final boolean depthEnabled;
    private final boolean alphaEnabled;
    private final boolean fogEnabled;
    private final boolean texture2dEnabled;
    private final boolean depthMask;
    private final boolean scissorEnabled;
    private final int[] scissorBox;
    private final boolean[] colorMask;
    private final float[] currentColor;
    private final int alphaFunc;
    private final float alphaRef;
    private final int[] viewport;
    private final boolean attribStackPushed;
    private final boolean matrixStacksPushed;
    private final boolean includeVertexArrayState;
    private final boolean restoreScreenState;
    private final @Nullable Framebuffer fallbackFramebuffer;
    private final int fallbackWidth;
    private final int fallbackHeight;

    private ActiniumGlStateGuard(boolean includeExtendedState,
                                 boolean restoreScreenState,
                                 @Nullable Framebuffer fallbackFramebuffer,
                                 int fallbackWidth,
                                 int fallbackHeight) {
        boolean pushedAttrib = false;
        boolean pushedMatrices = false;

        if (includeExtendedState) {
            try {
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                pushedAttrib = true;

                int capturedMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPushMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPushMatrix();
                GL11.glMatrixMode(GL11.GL_TEXTURE);
                GL11.glPushMatrix();
                GL11.glMatrixMode(capturedMatrixMode);
                pushedMatrices = true;
            } catch (RuntimeException ignored) {
            }
        }

        SCRATCH_COLOR_MASK_BUFFER.clear();
        ActiniumRenderPipeline.invokeGlGetInteger(GL11.GL_COLOR_WRITEMASK, SCRATCH_COLOR_MASK_BUFFER);
        SCRATCH_ALPHA_REF_BUFFER.clear();
        ActiniumRenderPipeline.invokeGlGetFloat(GL11.GL_ALPHA_TEST_REF, SCRATCH_ALPHA_REF_BUFFER);
        SCRATCH_CURRENT_COLOR_BUFFER.clear();
        ActiniumRenderPipeline.invokeGlGetFloat(GL11.GL_CURRENT_COLOR, SCRATCH_CURRENT_COLOR_BUFFER);
        SCRATCH_VIEWPORT_BUFFER.clear();
        ActiniumRenderPipeline.invokeGlGetInteger(GL11.GL_VIEWPORT, SCRATCH_VIEWPORT_BUFFER);

        if (includeExtendedState) {
            SCRATCH_LINE_WIDTH_BUFFER.clear();
            ActiniumRenderPipeline.invokeGlGetFloat(GL11.GL_LINE_WIDTH, SCRATCH_LINE_WIDTH_BUFFER);
            SCRATCH_POLYGON_MODE_BUFFER.clear();
            ActiniumRenderPipeline.invokeGlGetInteger(GL11.GL_POLYGON_MODE, SCRATCH_POLYGON_MODE_BUFFER);
            SCRATCH_SCISSOR_BOX_BUFFER.clear();
            ActiniumRenderPipeline.invokeGlGetInteger(GL11.GL_SCISSOR_BOX, SCRATCH_SCISSOR_BOX_BUFFER);
        } else {
            SCRATCH_LINE_WIDTH_BUFFER.clear();
            SCRATCH_LINE_WIDTH_BUFFER.put(0, 1.0f);
            SCRATCH_POLYGON_MODE_BUFFER.clear();
            SCRATCH_POLYGON_MODE_BUFFER.put(0, GL11.GL_FILL);
            SCRATCH_POLYGON_MODE_BUFFER.put(1, GL11.GL_FILL);
            SCRATCH_SCISSOR_BOX_BUFFER.clear();
            SCRATCH_SCISSOR_BOX_BUFFER.put(0, 0);
            SCRATCH_SCISSOR_BOX_BUFFER.put(1, 0);
            SCRATCH_SCISSOR_BOX_BUFFER.put(2, 0);
            SCRATCH_SCISSOR_BOX_BUFFER.put(3, 0);
        }

        int capturedActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int[] capturedTextureBindings = new int[ActiniumRenderPipeline.getTrackedTextureUnitCount()];

        for (int unit = 0; unit < capturedTextureBindings.length; unit++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            capturedTextureBindings[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }

        GL13.glActiveTexture(capturedActiveTexture);

        this.framebufferBinding = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        this.drawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
        this.readBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        this.activeTexture = capturedActiveTexture;
        this.textureBinding2d = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        this.textureBindings2d = capturedTextureBindings;
        this.currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        this.vertexArrayBinding = includeExtendedState ? GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING) : 0;
        this.arrayBufferBinding = includeExtendedState ? GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING) : 0;
        this.elementArrayBufferBinding = includeExtendedState ? GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING) : 0;
        this.matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        this.shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        this.polygonModeFront = SCRATCH_POLYGON_MODE_BUFFER.get(0);
        this.polygonModeBack = SCRATCH_POLYGON_MODE_BUFFER.get(1);
        this.lineWidth = SCRATCH_LINE_WIDTH_BUFFER.get(0);
        this.depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        this.blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        this.blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        this.blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        this.blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        this.cullFaceMode = includeExtendedState ? GL11.glGetInteger(GL11.GL_CULL_FACE_MODE) : GL11.GL_BACK;
        this.frontFace = includeExtendedState ? GL11.glGetInteger(GL11.GL_FRONT_FACE) : GL11.GL_CCW;
        this.blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        this.cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        this.depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        this.alphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        this.fogEnabled = GL11.glIsEnabled(GL11.GL_FOG);
        this.texture2dEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        this.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        this.scissorEnabled = includeExtendedState && GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        this.scissorBox = new int[]{
                SCRATCH_SCISSOR_BOX_BUFFER.get(0),
                SCRATCH_SCISSOR_BOX_BUFFER.get(1),
                SCRATCH_SCISSOR_BOX_BUFFER.get(2),
                SCRATCH_SCISSOR_BOX_BUFFER.get(3)
        };
        this.colorMask = new boolean[]{
                SCRATCH_COLOR_MASK_BUFFER.get(0) != 0,
                SCRATCH_COLOR_MASK_BUFFER.get(1) != 0,
                SCRATCH_COLOR_MASK_BUFFER.get(2) != 0,
                SCRATCH_COLOR_MASK_BUFFER.get(3) != 0
        };
        this.currentColor = new float[]{
                SCRATCH_CURRENT_COLOR_BUFFER.get(0),
                SCRATCH_CURRENT_COLOR_BUFFER.get(1),
                SCRATCH_CURRENT_COLOR_BUFFER.get(2),
                SCRATCH_CURRENT_COLOR_BUFFER.get(3)
        };
        this.alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        this.alphaRef = SCRATCH_ALPHA_REF_BUFFER.get(0);
        this.viewport = new int[]{
                SCRATCH_VIEWPORT_BUFFER.get(0),
                SCRATCH_VIEWPORT_BUFFER.get(1),
                SCRATCH_VIEWPORT_BUFFER.get(2),
                SCRATCH_VIEWPORT_BUFFER.get(3)
        };
        this.attribStackPushed = pushedAttrib;
        this.matrixStacksPushed = pushedMatrices;
        this.includeVertexArrayState = includeExtendedState;
        this.restoreScreenState = restoreScreenState;
        this.fallbackFramebuffer = fallbackFramebuffer;
        this.fallbackWidth = fallbackWidth;
        this.fallbackHeight = fallbackHeight;
    }

    static ActiniumGlStateGuard captureWorldStage() {
        return new ActiniumGlStateGuard(false, false, null, 0, 0);
    }

    static ActiniumGlStateGuard capturePipelinePass(@Nullable Framebuffer framebuffer, int width, int height, boolean restoreScreenState) {
        return new ActiniumGlStateGuard(true, restoreScreenState, framebuffer, width, height);
    }

    static ActiniumGlStateGuard captureShadowPass(@Nullable Framebuffer framebuffer, int width, int height) {
        return new ActiniumGlStateGuard(true, false, framebuffer, width, height);
    }

    void restore() {
        if (this.matrixStacksPushed) {
            try {
                GL11.glMatrixMode(GL11.GL_TEXTURE);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            } catch (RuntimeException ignored) {
            }
        }

        if (this.attribStackPushed) {
            try {
                GL11.glPopAttrib();
            } catch (RuntimeException ignored) {
            }
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferBinding);
        if (this.framebufferBinding != 0) {
            GL11.glDrawBuffer(this.drawBuffer);
            GL11.glReadBuffer(this.readBuffer);
        } else if (this.fallbackFramebuffer != null) {
            this.fallbackFramebuffer.bindFramebuffer(true);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        } else {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glDrawBuffer(this.drawBuffer);
            GL11.glReadBuffer(this.readBuffer);
        }

        GL20.glUseProgram(this.currentProgram);

        for (int unit = 0; unit < this.textureBindings2d.length; unit++) {
            ActiniumRenderPipeline.setActiveTextureUnit(unit);
            if (unit < ActiniumRenderPipeline.getGlStateManagerTextureUnitCount()) {
                GlStateManager.bindTexture(this.textureBindings2d[unit]);
            } else {
                ActiniumRenderPipeline.bindTextureDirect(this.textureBindings2d[unit]);
            }
        }

        ActiniumRenderPipeline.setActiveTextureEnum(this.activeTexture);
        if (this.activeTexture - OpenGlHelper.defaultTexUnit < ActiniumRenderPipeline.getGlStateManagerTextureUnitCount()
                && this.activeTexture - OpenGlHelper.defaultTexUnit >= 0) {
            GlStateManager.bindTexture(this.textureBinding2d);
        } else {
            ActiniumRenderPipeline.bindTextureDirect(this.textureBinding2d);
        }

        if (this.includeVertexArrayState) {
            GL30.glBindVertexArray(this.vertexArrayBinding);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.arrayBufferBinding);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.elementArrayBufferBinding);
        }

        GL11.glMatrixMode(this.matrixMode);
        GL11.glShadeModel(this.shadeModel);
        if (this.includeVertexArrayState) {
            GL11.glPolygonMode(GL11.GL_FRONT, this.polygonModeFront);
            GL11.glPolygonMode(GL11.GL_BACK, this.polygonModeBack);
            GL11.glLineWidth(this.lineWidth);
            GL11.glCullFace(this.cullFaceMode);
            GL11.glFrontFace(this.frontFace);
            setEnabled(GL11.GL_SCISSOR_TEST, this.scissorEnabled);
            GL11.glScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
        }

        GL11.glDepthFunc(this.depthFunc);
        GL14.glBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
        setEnabled(GL11.GL_BLEND, this.blendEnabled);
        setEnabled(GL11.GL_CULL_FACE, this.cullEnabled);
        setEnabled(GL11.GL_DEPTH_TEST, this.depthEnabled);
        setEnabled(GL11.GL_ALPHA_TEST, this.alphaEnabled);
        setEnabled(GL11.GL_FOG, this.fogEnabled);
        setEnabled(GL11.GL_TEXTURE_2D, this.texture2dEnabled);
        GL11.glDepthMask(this.depthMask);
        GL11.glColorMask(this.colorMask[0], this.colorMask[1], this.colorMask[2], this.colorMask[3]);
        GL11.glColor4f(this.currentColor[0], this.currentColor[1], this.currentColor[2], this.currentColor[3]);
        GL11.glAlphaFunc(this.alphaFunc, this.alphaRef);
        GL11.glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
        ActiniumRenderPipeline.syncGlStateManagerToCapturedState(
                this.activeTexture,
                this.textureBindings2d,
                this.blendEnabled,
                this.cullEnabled,
                this.depthEnabled,
                this.alphaEnabled,
                this.fogEnabled,
                this.texture2dEnabled,
                this.depthMask,
                this.colorMask,
                this.currentColor,
                this.alphaFunc,
                this.alphaRef,
                this.shadeModel,
                this.depthFunc,
                this.blendSrcRgb,
                this.blendDstRgb,
                this.blendSrcAlpha,
                this.blendDstAlpha,
                this.viewport[0],
                this.viewport[1],
                this.viewport[2],
                this.viewport[3],
                this.matrixMode
        );
    }

    @Override
    public void close() {
        this.restore();
        if (this.restoreScreenState && this.fallbackFramebuffer != null) {
            ActiniumRenderPipeline.restoreScreenRenderStateStatic(this.fallbackFramebuffer, this.fallbackWidth, this.fallbackHeight);
        }
    }

    private static void setEnabled(int capability, boolean enabled) {
        if (enabled) {
            GL11.glEnable(capability);
        } else {
            GL11.glDisable(capability);
        }
    }
}
