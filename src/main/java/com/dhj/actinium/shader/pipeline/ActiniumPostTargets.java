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
    public static final int TARGET_COLORTEX8 = 8;
    public static final int TARGET_COLORTEX9 = 9;
    public static final int TARGET_COLORTEX10 = 10;
    public static final int TARGET_COLORTEX11 = 11;
    public static final int TARGET_COLORTEX12 = 12;
    public static final int TARGET_COLORTEX13 = 13;
    public static final int TARGET_COLORTEX14 = 14;
    public static final int TARGET_COLORTEX15 = 15;
    public static final int TARGET_COUNT = 16;

    private final ColorFormat[] formats;
    private final TargetSettings[] settings;
    private final TargetSlot[] targets;
    private final int framebufferId;
    private final int copyReadFramebufferId;
    private final int copyDrawFramebufferId;
    private final int clearFramebufferId;
    private final int[] depthTextures = new int[3];
    private final int[] drawAttachmentScratch;
    private final IntBuffer drawBufferBuffer;
    private final FloatBuffer clearColorBuffer = BufferUtils.createFloatBuffer(4);

    private int maxColorAttachments;
    private int maxDrawBuffers;
    private int lastBoundAttachmentCount;
    private int width;
    private int height;

    ActiniumPostTargets(ColorFormat[] formats, TargetSettings[] settings) {
        this.formats = formats.clone();
        this.settings = settings.clone();
        this.targets = new TargetSlot[this.formats.length];
        this.drawAttachmentScratch = new int[this.targets.length];
        this.drawBufferBuffer = BufferUtils.createIntBuffer(this.targets.length);
        this.framebufferId = GL30.glGenFramebuffers();
        this.copyReadFramebufferId = GL30.glGenFramebuffers();
        this.copyDrawFramebufferId = GL30.glGenFramebuffers();
        this.clearFramebufferId = GL30.glGenFramebuffers();

        for (int i = 0; i < this.targets.length; i++) {
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

        for (int i = 0; i < this.targets.length; i++) {
            TargetSlot slot = this.targets[i];
            ColorFormat format = this.formats[Math.max(0, Math.min(i, this.formats.length - 1))];
            slot.mainTexture = createColorTexture(width, height, format);
            slot.altTexture = createColorTexture(width, height, format);
            slot.sourceIsAlt = false;
            this.clearTargetTexture(slot.mainTexture, width, height, this.resolveSettings(i));
            this.clearTargetTexture(slot.altTexture, width, height, this.resolveSettings(i));
        }

        deleteTextures(this.depthTextures);
        this.depthTextures[0] = createDepthTexture(width, height);
        this.depthTextures[1] = createDepthTexture(width, height);
        this.depthTextures[2] = createDepthTexture(width, height);
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
        this.copyTexture(sceneTexture, this.targets[TARGET_COLORTEX0].getSourceTexture(), this.width, this.height);
        this.copyTexture(sceneTexture, this.targets[TARGET_COLORTEX1].getSourceTexture(), this.width, this.height);
        this.copyDepthTexture(mainFramebuffer, 0);
        this.copyDepthTexture(mainFramebuffer, 2);
    }

    public void copySceneInputs(Framebuffer mainFramebuffer, @Nullable Integer gaux4Texture) {
        this.copySceneInputs(mainFramebuffer);

        if (gaux4Texture != null && gaux4Texture > 0) {
            this.copyTexture(gaux4Texture, this.targets[TARGET_GAUX4].getSourceTexture(), this.width, this.height);
        }
    }

    public void copyPostSceneInputs(Framebuffer mainFramebuffer, @Nullable Integer gaux4Texture) {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        int sceneTexture = mainFramebuffer.framebufferTexture;
        this.copyTexture(sceneTexture, this.targets[TARGET_COLORTEX0].getSourceTexture(), this.width, this.height);
        this.copyDepthTexture(mainFramebuffer, 0);
        this.copyDepthTexture(mainFramebuffer, 2);

        if (gaux4Texture != null && gaux4Texture > 0) {
            this.copyTexture(gaux4Texture, this.targets[TARGET_GAUX4].getSourceTexture(), this.width, this.height);
        }
    }

    public void copySceneTextures(Framebuffer mainFramebuffer, @Nullable Integer worldGaux4Texture) {
        this.copySceneColors(mainFramebuffer, worldGaux4Texture);

        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        this.copyDepthTexture(mainFramebuffer, 0);
        this.copyDepthTexture(mainFramebuffer, 2);
    }

    public void copySceneColors(Framebuffer mainFramebuffer, @Nullable Integer worldGaux4Texture) {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        int sceneTexture = mainFramebuffer.framebufferTexture;
        this.prepareFramePersistentTargets();

        this.copyTexture(sceneTexture, this.targets[TARGET_COLORTEX0].mainTexture, this.width, this.height);
        // Post programs expect colortex1 to contain the current fully rendered scene.
        // World-stage color targets may only contain sky/translucent intermediates.
        this.copyTexture(sceneTexture, this.targets[TARGET_COLORTEX1].mainTexture, this.width, this.height);
        this.targets[TARGET_COLORTEX0].sourceIsAlt = false;
        this.targets[TARGET_COLORTEX1].sourceIsAlt = false;

        clearTargetIfRequested(TARGET_COLORTEX2);
        clearTargetIfRequested(TARGET_GAUX1);
        clearTargetIfRequested(TARGET_GAUX2);
        clearTargetIfRequested(TARGET_GAUX3);

        if (worldGaux4Texture != null && worldGaux4Texture > 0) {
            this.copyTexture(worldGaux4Texture, this.targets[TARGET_GAUX4].mainTexture, this.width, this.height);
        } else if (this.resolveSettings(TARGET_GAUX4).clear()) {
            this.clearTargetTexture(this.targets[TARGET_GAUX4].mainTexture, this.width, this.height, this.resolveSettings(TARGET_GAUX4));
        }
        this.targets[TARGET_GAUX4].sourceIsAlt = false;

        clearTargetAltIfRequested(TARGET_COLORTEX0);
        clearTargetAltIfRequested(TARGET_COLORTEX1);
        clearTargetAltIfRequested(TARGET_COLORTEX2);
        clearTargetAltIfRequested(TARGET_GAUX1);
        clearTargetAltIfRequested(TARGET_GAUX2);
        clearTargetAltIfRequested(TARGET_GAUX3);
        clearTargetAltIfRequested(TARGET_GAUX4);
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
        this.copyDepthTexture(mainFramebuffer, 2);
    }

    public void copyCurrentDepth(Framebuffer mainFramebuffer, int index) {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        this.copyDepthTexture(mainFramebuffer, index);
    }

    public void copyDepthTextureToSlot(int sourceTexture, int index) {
        if (this.width <= 0 || this.height <= 0 || sourceTexture <= 0) {
            return;
        }

        int resolvedIndex = Math.max(0, Math.min(index, this.depthTextures.length - 1));
        this.copyDepthTexture(sourceTexture, this.depthTextures[resolvedIndex], this.width, this.height);
    }

    public void copyTargetsFrom(@Nullable ActiniumPostTargets other, int[] targetIndices) {
        if (other == null || this.width <= 0 || this.height <= 0 || targetIndices == null || targetIndices.length == 0) {
            return;
        }

        for (int targetIndex : targetIndices) {
            if (targetIndex < 0 || targetIndex >= this.targets.length) {
                continue;
            }

            int sourceTexture = other.getSourceTexture(targetIndex);
            if (sourceTexture != 0) {
                this.copyTexture(sourceTexture, this.targets[targetIndex].mainTexture, this.width, this.height);
            } else {
                this.clearTargetTexture(this.targets[targetIndex].mainTexture, this.width, this.height, this.resolveSettings(targetIndex));
            }
            this.targets[targetIndex].sourceIsAlt = false;
        }
    }

    private void prepareFramePersistentTargets() {
        for (int targetIndex = 0; targetIndex < this.targets.length; targetIndex++) {
            TargetSlot slot = this.targets[targetIndex];

            TargetSettings settings = this.resolveSettings(targetIndex);

            if (!settings.clear()) {
                int previousSource = slot.getSourceTexture();

                if (previousSource != 0 && previousSource != slot.mainTexture) {
                    this.copyTexture(previousSource, slot.mainTexture, this.width, this.height);
                }

                slot.sourceIsAlt = false;
                continue;
            }

            slot.sourceIsAlt = false;
            this.clearTargetTexture(slot.mainTexture, this.width, this.height, settings);
            this.clearTargetTexture(slot.altTexture, this.width, this.height, settings);
        }
    }

    private void clearTargetIfRequested(int targetIndex) {
        TargetSettings settings = this.resolveSettings(targetIndex);
        if (settings.clear()) {
            this.clearTargetTexture(this.targets[targetIndex].mainTexture, this.width, this.height, settings);
        }
    }

    private void clearTargetAltIfRequested(int targetIndex) {
        TargetSettings settings = this.resolveSettings(targetIndex);
        if (settings.clear()) {
            this.clearTargetTexture(this.targets[targetIndex].altTexture, this.width, this.height, settings);
        }
    }

    private TargetSettings resolveSettings(int targetIndex) {
        return this.settings[Math.max(0, Math.min(targetIndex, this.settings.length - 1))];
    }

    public void bindWriteFramebuffer(int[] drawBuffers) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferId);
        int supportedAttachmentCount = this.getSupportedAttachmentCount();

        if (drawBuffers.length > supportedAttachmentCount) {
            throw new IllegalArgumentException("Actinium post draw buffer count " + drawBuffers.length
                    + " exceeds supported attachment count " + supportedAttachmentCount);
        }

        this.drawBufferBuffer.clear();
        int attachmentCount = 0;

        for (int targetIndex : drawBuffers) {
            validateTargetIndex(targetIndex, this.targets.length);
            int attachment = GL30.GL_COLOR_ATTACHMENT0 + attachmentCount;
            int textureId = this.targets[targetIndex].getWriteTexture();
            this.drawAttachmentScratch[attachmentCount] = attachment;
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D, textureId, 0);
            this.drawBufferBuffer.put(attachment);
            attachmentCount++;
        }

        for (int i = attachmentCount; i < Math.min(this.lastBoundAttachmentCount, supportedAttachmentCount); i++) {
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + i, GL11.GL_TEXTURE_2D, 0, 0);
        }

        this.drawBufferBuffer.flip();
        GL20.glDrawBuffers(this.drawBufferBuffer);
        this.lastBoundAttachmentCount = attachmentCount;

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

    public void flipTarget(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= this.targets.length) {
            return;
        }

        this.targets[targetIndex].sourceIsAlt = !this.targets[targetIndex].sourceIsAlt;
    }

    public boolean isTargetFlipped(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= this.targets.length) {
            return false;
        }

        return this.targets[targetIndex].sourceIsAlt;
    }

    public void setTargetFlipped(int targetIndex, boolean flipped) {
        if (targetIndex < 0 || targetIndex >= this.targets.length) {
            return;
        }

        this.targets[targetIndex].sourceIsAlt = flipped;
    }

    public int getSourceTexture(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= this.targets.length) {
            return 0;
        }

        return this.targets[targetIndex].getSourceTexture();
    }

    public int getInternalFormat(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= this.formats.length) {
            return ColorFormat.RGBA8.internalFormat();
        }

        return this.formats[targetIndex].internalFormat();
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
        if (this.copyReadFramebufferId != 0) {
            GL30.glDeleteFramebuffers(this.copyReadFramebufferId);
        }
        if (this.copyDrawFramebufferId != 0) {
            GL30.glDeleteFramebuffers(this.copyDrawFramebufferId);
        }
        if (this.clearFramebufferId != 0) {
            GL30.glDeleteFramebuffers(this.clearFramebufferId);
        }
    }

    private void copyDepthTexture(Framebuffer mainFramebuffer, int index) {
        int resolvedIndex = Math.max(0, Math.min(index, this.depthTextures.length - 1));
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        try {
            mainFramebuffer.bindFramebuffer(true);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.depthTextures[resolvedIndex]);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, this.width, this.height);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
        }
    }

    private void copyDepthTexture(int sourceTexture, int destinationTexture, int width, int height) {
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDrawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);

        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.copyReadFramebufferId);
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, sourceTexture, 0);

            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.copyDrawFramebufferId);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, destinationTexture, 0);

            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glDrawBuffer(previousDrawBuffer);
            GL11.glReadBuffer(previousReadBuffer);
        }
    }

    private void copyTexture(int sourceTexture, int destinationTexture, int width, int height) {
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDrawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);

        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.copyReadFramebufferId);
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, sourceTexture, 0);
            GL30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);

            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.copyDrawFramebufferId);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, destinationTexture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);

            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glDrawBuffer(previousDrawBuffer);
            GL11.glReadBuffer(previousReadBuffer);
        }
    }

    private void clearTargetTexture(int textureId, int width, int height, TargetSettings settings) {
        this.clearColorTexture(textureId, width, height, settings.clearRed(), settings.clearGreen(), settings.clearBlue(), settings.clearAlpha());
    }

    private void clearColorTexture(int textureId, int width, int height, float red, float green, float blue, float alpha) {
        ActiniumRenderPipeline.clearGlErrorsSilently();
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousDrawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);

        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.clearFramebufferId);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, 0);
            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);

            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Incomplete Actinium post clear framebuffer: " + status);
            }

            this.clearColorBuffer.clear();
            this.clearColorBuffer.put(red).put(green).put(blue).put(alpha);
            this.clearColorBuffer.flip();
            GL30.glClearBufferfv(GL11.GL_COLOR, 0, this.clearColorBuffer);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glDrawBuffer(previousDrawBuffer);
            GL11.glReadBuffer(previousReadBuffer);
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

    private int getSupportedAttachmentCount() {
        if (this.maxColorAttachments <= 0) {
            this.maxColorAttachments = Math.max(1, GL11.glGetInteger(GL30.GL_MAX_COLOR_ATTACHMENTS));
        }

        if (this.maxDrawBuffers <= 0) {
            this.maxDrawBuffers = Math.max(1, GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS));
        }

        return Math.min(Math.min(this.maxColorAttachments, this.maxDrawBuffers), this.targets.length);
    }

    private static void validateTargetIndex(int targetIndex, int targetCount) {
        if (targetIndex < 0 || targetIndex >= targetCount) {
            throw new IllegalArgumentException("Unsupported Actinium post draw buffer target " + targetIndex);
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

    record TargetSettings(boolean clear, float clearRed, float clearGreen, float clearBlue, float clearAlpha) {
    }
}
