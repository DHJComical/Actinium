package com.dhj.actinium.shader.pipeline;

import net.minecraft.client.shader.Framebuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

final class ActiniumPostTargets {
    public static final int TARGET_COLORTEX0 = 0;
    public static final int TARGET_COLORTEX1 = 1;
    public static final int TARGET_COLORTEX2 = 2;
    public static final int TARGET_COLORTEX3 = 3;
    public static final int TARGET_GAUX1 = 4;
    public static final int TARGET_GAUX2 = 5;
    public static final int TARGET_GAUX3 = 6;
    public static final int TARGET_GAUX4 = 7;
    public static final int TARGET_COUNT = 8;
    private static final boolean[] FRAME_PERSISTENT_TARGETS = {
            false, // colortex0
            false, // colortex1
            false, // colortex2
            true,  // colortex3 (TAA history)
            false, // gaux1
            false, // gaux2
            true,  // gaux3 (exposure history)
            false  // gaux4
    };

    private final ColorFormat[] formats;
    private final TargetSlot[] targets = new TargetSlot[TARGET_COUNT];
    private final int framebufferId;
    private final int[] depthTextures = new int[2];
    private final int[] drawAttachmentScratch = new int[TARGET_COUNT];
    private final IntBuffer drawBufferBuffer = BufferUtils.createIntBuffer(TARGET_COUNT);

    private int width;
    private int height;

    ActiniumPostTargets(ColorFormat[] formats) {
        this.formats = formats.clone();
        this.framebufferId = GL30.glGenFramebuffers();

        for (int i = 0; i < TARGET_COUNT; i++) {
            this.targets[i] = new TargetSlot();
        }
    }

    public void ensureSize(int width, int height) {
        if (this.width == width && this.height == height && this.targets[0].mainTexture != 0) {
            return;
        }

        this.width = width;
        this.height = height;

        for (TargetSlot slot : this.targets) {
            slot.delete();
        }

        for (int i = 0; i < TARGET_COUNT; i++) {
            TargetSlot slot = this.targets[i];
            ColorFormat format = this.formats[Math.max(0, Math.min(i, this.formats.length - 1))];
            slot.mainTexture = createColorTexture(width, height, format);
            slot.altTexture = createColorTexture(width, height, format);
            slot.sourceIsAlt = false;
            clearColorTexture(slot.mainTexture, width, height);
            clearColorTexture(slot.altTexture, width, height);
        }

        deleteTextures(this.depthTextures);
        this.depthTextures[0] = createDepthTexture(width, height);
        this.depthTextures[1] = createDepthTexture(width, height);
    }

    public void resetSources() {
        for (TargetSlot slot : this.targets) {
            slot.sourceIsAlt = false;
        }
    }

    public void copySceneInputs(Framebuffer mainFramebuffer) {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        int sceneTexture = mainFramebuffer.framebufferTexture;
        copyTexture(sceneTexture, this.targets[TARGET_COLORTEX0].getSourceTexture(), this.width, this.height);
        copyTexture(sceneTexture, this.targets[TARGET_COLORTEX1].getSourceTexture(), this.width, this.height);
        this.copyDepthTexture(mainFramebuffer, 0);
    }

    public void copySceneInputs(Framebuffer mainFramebuffer, @Nullable Integer gaux4Texture) {
        this.copySceneInputs(mainFramebuffer);

        if (gaux4Texture != null && gaux4Texture > 0) {
            copyTexture(gaux4Texture, this.targets[TARGET_GAUX4].getSourceTexture(), this.width, this.height);
        }
    }

    public void copyPostSceneInputs(Framebuffer mainFramebuffer, @Nullable Integer gaux4Texture) {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        int sceneTexture = mainFramebuffer.framebufferTexture;
        copyTexture(sceneTexture, this.targets[TARGET_COLORTEX0].getSourceTexture(), this.width, this.height);
        this.copyDepthTexture(mainFramebuffer, 0);

        if (gaux4Texture != null && gaux4Texture > 0) {
            copyTexture(gaux4Texture, this.targets[TARGET_GAUX4].getSourceTexture(), this.width, this.height);
        }
    }

    public void copySceneTextures(Framebuffer mainFramebuffer, @Nullable Integer worldColorTexture, @Nullable Integer worldGaux4Texture) {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        int sceneTexture = mainFramebuffer.framebufferTexture;
        this.prepareFramePersistentTargets();

        copyTexture(sceneTexture, this.targets[TARGET_COLORTEX0].mainTexture, this.width, this.height);
        // Post programs expect colortex1 to contain the current fully rendered scene.
        // World-stage color targets may only contain sky/translucent intermediates.
        copyTexture(sceneTexture, this.targets[TARGET_COLORTEX1].mainTexture, this.width, this.height);
        this.targets[TARGET_COLORTEX0].sourceIsAlt = false;
        this.targets[TARGET_COLORTEX1].sourceIsAlt = false;

        clearColorTexture(this.targets[TARGET_COLORTEX2].mainTexture, this.width, this.height);
        clearColorTexture(this.targets[TARGET_GAUX1].mainTexture, this.width, this.height);
        clearColorTexture(this.targets[TARGET_GAUX2].mainTexture, this.width, this.height);

        if (worldGaux4Texture != null && worldGaux4Texture > 0) {
            copyTexture(worldGaux4Texture, this.targets[TARGET_GAUX4].mainTexture, this.width, this.height);
        } else {
            clearColorTexture(this.targets[TARGET_GAUX4].mainTexture, this.width, this.height);
        }
        this.targets[TARGET_GAUX4].sourceIsAlt = false;

        clearColorTexture(this.targets[TARGET_COLORTEX0].altTexture, this.width, this.height);
        clearColorTexture(this.targets[TARGET_COLORTEX1].altTexture, this.width, this.height);
        clearColorTexture(this.targets[TARGET_COLORTEX2].altTexture, this.width, this.height);
        clearColorTexture(this.targets[TARGET_GAUX1].altTexture, this.width, this.height);
        clearColorTexture(this.targets[TARGET_GAUX2].altTexture, this.width, this.height);
        clearColorTexture(this.targets[TARGET_GAUX4].altTexture, this.width, this.height);

        this.copyDepthTexture(mainFramebuffer, 0);
    }

    public void copyPreTranslucentDepth(Framebuffer mainFramebuffer) {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        this.copyDepthTexture(mainFramebuffer, 1);
    }

    public void copyCurrentDepthToAll(Framebuffer mainFramebuffer) {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        this.copyDepthTexture(mainFramebuffer, 0);
        this.copyDepthTexture(mainFramebuffer, 1);
    }

    private void prepareFramePersistentTargets() {
        for (int targetIndex = 0; targetIndex < TARGET_COUNT; targetIndex++) {
            TargetSlot slot = this.targets[targetIndex];

            if (FRAME_PERSISTENT_TARGETS[targetIndex]) {
                int previousSource = slot.getSourceTexture();

                if (previousSource != 0 && previousSource != slot.mainTexture) {
                    copyTexture(previousSource, slot.mainTexture, this.width, this.height);
                }

                slot.sourceIsAlt = false;
                clearColorTexture(slot.altTexture, this.width, this.height, getDefaultClearRed(targetIndex), getDefaultClearGreen(targetIndex), getDefaultClearBlue(targetIndex), getDefaultClearAlpha(targetIndex));
                continue;
            }

            slot.sourceIsAlt = false;
        }
    }

    private static float getDefaultClearRed(int targetIndex) {
        return targetIndex == TARGET_GAUX3 ? 1.0f : 0.0f;
    }

    private static float getDefaultClearGreen(int targetIndex) {
        return targetIndex == TARGET_GAUX3 ? 1.0f : 0.0f;
    }

    private static float getDefaultClearBlue(int targetIndex) {
        return targetIndex == TARGET_GAUX3 ? 1.0f : 0.0f;
    }

    private static float getDefaultClearAlpha(int targetIndex) {
        return targetIndex == TARGET_GAUX3 ? 1.0f : 0.0f;
    }

    public void bindWriteFramebuffer(int[] drawBuffers) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferId);

        this.drawBufferBuffer.clear();
        int attachmentCount = 0;

        for (int targetIndex : drawBuffers) {
            int attachment = GL30.GL_COLOR_ATTACHMENT0 + attachmentCount;
            int textureId = this.targets[targetIndex].getWriteTexture();
            this.drawAttachmentScratch[attachmentCount] = attachment;
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D, textureId, 0);
            this.drawBufferBuffer.put(attachment);
            attachmentCount++;
        }

        for (int i = attachmentCount; i < TARGET_COUNT; i++) {
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + i, GL11.GL_TEXTURE_2D, 0, 0);
        }

        this.drawBufferBuffer.flip();
        GL20.glDrawBuffers(this.drawBufferBuffer);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Incomplete Actinium post framebuffer: " + status + " for draw buffers " + Arrays.toString(drawBuffers));
        }
    }

    public void flipWrittenTargets(int[] drawBuffers) {
        for (int targetIndex : drawBuffers) {
            this.targets[targetIndex].sourceIsAlt = !this.targets[targetIndex].sourceIsAlt;
        }
    }

    public int getSourceTexture(int targetIndex) {
        return this.targets[targetIndex].getSourceTexture();
    }

    public int getDepthTexture(int index) {
        return this.depthTextures[Math.max(0, Math.min(index, this.depthTextures.length - 1))];
    }

    public void bindMainFramebuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void delete() {
        for (TargetSlot slot : this.targets) {
            slot.delete();
        }

        deleteTextures(this.depthTextures);

        if (this.framebufferId != 0) {
            GL30.glDeleteFramebuffers(this.framebufferId);
        }
    }

    private void copyDepthTexture(Framebuffer mainFramebuffer, int index) {
        int resolvedIndex = Math.max(0, Math.min(index, this.depthTextures.length - 1));
        mainFramebuffer.bindFramebuffer(true);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.depthTextures[resolvedIndex]);
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, 0, 0, this.width, this.height, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private static void copyTexture(int sourceTexture, int destinationTexture, int width, int height) {
        int readFramebuffer = GL30.glGenFramebuffers();
        int drawFramebuffer = GL30.glGenFramebuffers();

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, sourceTexture, 0);
        GL30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, destinationTexture, 0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);

        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
        GL30.glDeleteFramebuffers(readFramebuffer);
        GL30.glDeleteFramebuffers(drawFramebuffer);
    }

    private static void clearColorTexture(int textureId, int width, int height) {
        clearColorTexture(textureId, width, height, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    private static void clearColorTexture(int textureId, int width, int height, float red, float green, float blue, float alpha) {
        ActiniumRenderPipeline.clearGlErrorsSilently();
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousDrawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int framebuffer = GL30.glGenFramebuffers();

        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, 0);
            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);

            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Incomplete Actinium post clear framebuffer: " + status);
            }

            FloatBuffer clearColor = BufferUtils.createFloatBuffer(4);
            clearColor.put(red).put(green).put(blue).put(alpha);
            clearColor.flip();
            GL30.glClearBufferfv(GL11.GL_COLOR, 0, clearColor);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glDrawBuffer(previousDrawBuffer);
            GL11.glReadBuffer(previousReadBuffer);
            GL30.glDeleteFramebuffers(framebuffer);
        }
    }

    private static int createColorTexture(int width, int height, ColorFormat format) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, format.internalFormat, width, height, 0, format.pixelFormat, format.pixelType, 0L);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    private static int createDepthTexture(int width, int height) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0L);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    private static void deleteTextures(int[] textures) {
        for (int i = 0; i < textures.length; i++) {
            if (textures[i] != 0) {
                GL11.glDeleteTextures(textures[i]);
                textures[i] = 0;
            }
        }
    }

    private static final class TargetSlot {
        private int mainTexture;
        private int altTexture;
        private boolean sourceIsAlt;

        private int getSourceTexture() {
            return this.sourceIsAlt ? this.altTexture : this.mainTexture;
        }

        private int getWriteTexture() {
            return this.sourceIsAlt ? this.mainTexture : this.altTexture;
        }

        private void delete() {
            if (this.mainTexture != 0) {
                GL11.glDeleteTextures(this.mainTexture);
                this.mainTexture = 0;
            }

            if (this.altTexture != 0) {
                GL11.glDeleteTextures(this.altTexture);
                this.altTexture = 0;
            }

            this.sourceIsAlt = false;
        }
    }

    enum ColorFormat {
        R8(GL30.GL_R8, GL30.GL_RED, GL11.GL_UNSIGNED_BYTE),
        RG8(GL30.GL_RG8, GL30.GL_RG, GL11.GL_UNSIGNED_BYTE),
        RGB8(GL11.GL_RGB8, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE),
        RGBA8(GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE),
        RGB10_A2(GL11.GL_RGB10_A2, GL11.GL_RGBA, GL12.GL_UNSIGNED_INT_10_10_10_2),
        RG16F(GL30.GL_RG16F, GL30.GL_RG, GL11.GL_FLOAT),
        RGB16F(GL30.GL_RGB16F, GL11.GL_RGB, GL11.GL_FLOAT),
        RGBA16F(GL30.GL_RGBA16F, GL11.GL_RGBA, GL11.GL_FLOAT),
        R16F(GL30.GL_R16F, GL30.GL_RED, GL11.GL_FLOAT),
        R32F(GL30.GL_R32F, GL30.GL_RED, GL11.GL_FLOAT),
        RG32F(GL30.GL_RG32F, GL30.GL_RG, GL11.GL_FLOAT),
        RGBA32F(GL30.GL_RGBA32F, GL11.GL_RGBA, GL11.GL_FLOAT),
        R11F_G11F_B10F(GL30.GL_R11F_G11F_B10F, GL11.GL_RGB, GL11.GL_FLOAT);

        private final int internalFormat;
        private final int pixelFormat;
        private final int pixelType;

        ColorFormat(int internalFormat, int pixelFormat, int pixelType) {
            this.internalFormat = internalFormat;
            this.pixelFormat = pixelFormat;
            this.pixelType = pixelType;
        }

        int internalFormat() {
            return this.internalFormat;
        }

        int pixelFormat() {
            return this.pixelFormat;
        }

        int pixelType() {
            return this.pixelType;
        }
    }
}
