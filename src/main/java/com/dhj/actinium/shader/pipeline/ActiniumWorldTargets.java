package com.dhj.actinium.shader.pipeline;

import net.minecraft.client.shader.Framebuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

final class ActiniumWorldTargets {
    private static final int[] SUPPORTED_TARGETS = {0, 1, 7};

    private final ActiniumPostTargets.ColorFormat colorTexture1Format;
    private final ActiniumPostTargets.ColorFormat gaux4Format;
    private final int framebufferId;
    private final IntBuffer drawBufferBuffer = BufferUtils.createIntBuffer(SUPPORTED_TARGETS.length);
    private final TargetSlot colorTexture1 = new TargetSlot();
    private final TargetSlot gaux4Texture = new TargetSlot();
    private int width;
    private int height;

    ActiniumWorldTargets(ActiniumPostTargets.ColorFormat colorTexture1Format, ActiniumPostTargets.ColorFormat gaux4Format) {
        this.colorTexture1Format = colorTexture1Format;
        this.gaux4Format = gaux4Format;
        this.framebufferId = GL30.glGenFramebuffers();
    }

    public void ensureSize(int width, int height) {
        if (this.width == width && this.height == height && this.gaux4Texture.mainTexture != 0) {
            return;
        }

        this.deleteTextures();
        this.width = width;
        this.height = height;
        this.colorTexture1.allocate(width, height, this.colorTexture1Format);
        this.gaux4Texture.allocate(width, height, this.gaux4Format);
        ActiniumRenderPipeline.debugCheckGlErrors("world-targets.ensureSize");
    }

    public void beginFrame(Framebuffer mainFramebuffer, float red, float green, float blue, float alpha) {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        this.colorTexture1.reset(this.width, this.height, red, green, blue, alpha);
        this.gaux4Texture.reset(this.width, this.height, red, green, blue, alpha);
        ActiniumRenderPipeline.debugCheckGlErrors("world-targets.beginFrame");
    }

    public void bindWriteFramebuffer(Framebuffer mainFramebuffer, int[] drawBuffers, boolean renderColorTex1ToMain) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferId);
        this.drawBufferBuffer.clear();

        for (int i = 0; i < drawBuffers.length; i++) {
            int targetIndex = drawBuffers[i];
            validateTarget(targetIndex);
            int attachment = GL30.GL_COLOR_ATTACHMENT0 + i;
            int textureId = this.resolveTargetTexture(mainFramebuffer, targetIndex, renderColorTex1ToMain);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D, textureId, 0);
            this.drawBufferBuffer.put(attachment);
        }

        for (int i = drawBuffers.length; i < SUPPORTED_TARGETS.length; i++) {
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + i, GL11.GL_TEXTURE_2D, 0, 0);
        }

        if (mainFramebuffer.depthBuffer > -1) {
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, mainFramebuffer.depthBuffer);
        } else {
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, 0);
        }

        this.drawBufferBuffer.flip();
        GL20.glDrawBuffers(this.drawBufferBuffer);
        ActiniumRenderPipeline.debugCheckGlErrors("world-targets.drawBuffers" + Arrays.toString(drawBuffers));

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Incomplete Actinium world framebuffer: " + status + " for draw buffers " + Arrays.toString(drawBuffers));
        }
    }

    public void endWrite(Framebuffer mainFramebuffer, int[] drawBuffers, boolean renderColorTex1ToMain) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mainFramebuffer.framebufferObject);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        ActiniumRenderPipeline.debugCheckGlErrors("world-targets.endWrite");

        if (!renderColorTex1ToMain && containsTarget(drawBuffers, 1)) {
            this.colorTexture1.markWritten();
        }

        if (containsTarget(drawBuffers, 7)) {
            this.gaux4Texture.flip();
        }
    }

    public boolean hasSourceColorTexture() {
        return this.colorTexture1.wroteThisFrame;
    }

    public int getSourceColorTexture() {
        return this.colorTexture1.getStableTexture();
    }

    public int getSourceGaux4Texture() {
        return this.gaux4Texture.getSourceTexture();
    }

    public int getSourceGaux4TextureOrDefault(@Nullable Integer fallbackTexture) {
        int texture = this.getSourceGaux4Texture();
        return texture != 0 ? texture : fallbackTexture != null ? fallbackTexture : 0;
    }

    public void delete() {
        this.deleteTextures();

        if (this.framebufferId != 0) {
            GL30.glDeleteFramebuffers(this.framebufferId);
        }

        this.width = 0;
        this.height = 0;
    }

    private int resolveTargetTexture(Framebuffer mainFramebuffer, int targetIndex, boolean renderColorTex1ToMain) {
        return switch (targetIndex) {
            case 0 -> mainFramebuffer.framebufferTexture;
            case 1 -> renderColorTex1ToMain ? mainFramebuffer.framebufferTexture : this.colorTexture1.getStableTexture();
            case 7 -> this.gaux4Texture.getWriteTexture();
            default -> throw new IllegalArgumentException("Unsupported Actinium world target " + targetIndex);
        };
    }

    private static boolean containsTarget(int[] drawBuffers, int target) {
        for (int drawBuffer : drawBuffers) {
            if (drawBuffer == target) {
                return true;
            }
        }

        return false;
    }

    private static void validateTarget(int targetIndex) {
        for (int supportedTarget : SUPPORTED_TARGETS) {
            if (supportedTarget == targetIndex) {
                return;
            }
        }

        throw new IllegalArgumentException("Unsupported Actinium world draw buffer target " + targetIndex);
    }

    private void deleteTextures() {
        this.colorTexture1.delete();
        this.gaux4Texture.delete();
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
                throw new RuntimeException("Incomplete Actinium world clear framebuffer: " + status);
            }

            FloatBuffer clearColor = BufferUtils.createFloatBuffer(4);
            clearColor.put(red).put(green).put(blue).put(alpha);
            clearColor.flip();
            GL30.glClearBufferfv(GL11.GL_COLOR, 0, clearColor);
            ActiniumRenderPipeline.debugCheckGlErrors("world-targets.clearColorTexture.clear");
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glDrawBuffer(previousDrawBuffer);
            GL11.glReadBuffer(previousReadBuffer);
            GL30.glDeleteFramebuffers(framebuffer);
            ActiniumRenderPipeline.debugCheckGlErrors("world-targets.clearColorTexture.restore");
        }
    }

    private static int createColorTexture(int width, int height, ActiniumPostTargets.ColorFormat format) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, format.internalFormat(), width, height, 0, format.pixelFormat(), format.pixelType(), 0L);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    private static final class TargetSlot {
        private int mainTexture;
        private int altTexture;
        private boolean sourceIsAlt;
        private boolean wroteThisFrame;

        private void allocate(int width, int height, ActiniumPostTargets.ColorFormat format) {
            this.mainTexture = createColorTexture(width, height, format);
            this.altTexture = createColorTexture(width, height, format);
            this.reset(width, height, 0.0F, 0.0F, 0.0F, 1.0F);
        }

        private void reset(int width, int height, float red, float green, float blue, float alpha) {
            this.sourceIsAlt = false;
            this.wroteThisFrame = false;
            clearColorTexture(this.mainTexture, width, height, red, green, blue, alpha);
            clearColorTexture(this.altTexture, width, height, red, green, blue, alpha);
        }

        private int getSourceTexture() {
            return this.sourceIsAlt ? this.altTexture : this.mainTexture;
        }

        private int getStableTexture() {
            return this.mainTexture;
        }

        private int getWriteTexture() {
            return this.sourceIsAlt ? this.mainTexture : this.altTexture;
        }

        private void flip() {
            this.sourceIsAlt = !this.sourceIsAlt;
            this.wroteThisFrame = true;
        }

        private void markWritten() {
            this.wroteThisFrame = true;
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
            this.wroteThisFrame = false;
        }
    }
}
