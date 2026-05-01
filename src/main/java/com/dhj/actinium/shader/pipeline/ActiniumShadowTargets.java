package com.dhj.actinium.shader.pipeline;

import lombok.Getter;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

final class ActiniumShadowTargets {
    private final int[] depthTextures = new int[2];
    private int framebufferId;
    @Getter
    private int colorTexture;
    private int resolution;
    private Boolean hardwareFiltering;

    public void ensureSize(int resolution) {
        if (this.resolution == resolution && this.depthTextures[0] != 0) {
            return;
        }

        this.delete();
        this.resolution = resolution;
        this.hardwareFiltering = null;
        this.depthTextures[0] = createDepthTexture(resolution);
        this.depthTextures[1] = createDepthTexture(resolution);
        this.colorTexture = createColorTexture(resolution);
        this.framebufferId = GL30.glGenFramebuffers();
    }

    public void configureSampling(boolean hardwareFiltering) {
        if (this.hardwareFiltering != null && this.hardwareFiltering == hardwareFiltering) {
            return;
        }

        int filter = hardwareFiltering ? GL11.GL_LINEAR : GL11.GL_NEAREST;

        for (int depthTexture : this.depthTextures) {
            if (depthTexture == 0) {
                continue;
            }

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        this.hardwareFiltering = hardwareFiltering;
    }

    public void updatePlaceholderFromMainFramebuffer(Framebuffer mainFramebuffer) {
        if (this.resolution <= 0) {
            return;
        }

        mainFramebuffer.bindFramebuffer(true);

        for (int depthTexture : this.depthTextures) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, 0, 0,
                    Math.clamp(mainFramebuffer.framebufferWidth, 1, this.resolution),
                    Math.clamp(mainFramebuffer.framebufferHeight, 1, this.resolution), 0);
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.colorTexture);
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 0, 0,
                Math.clamp(mainFramebuffer.framebufferWidth, 1, this.resolution),
                Math.clamp(mainFramebuffer.framebufferHeight, 1, this.resolution), 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void clear() {
        if (this.framebufferId == 0 || this.resolution <= 0) {
            return;
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.colorTexture, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.depthTextures[0], 0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glViewport(0, 0, this.resolution, this.resolution);
        GL11.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        this.copyDepthPrimaryToSecondary();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void beginWrite() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.colorTexture, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.depthTextures[0], 0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glViewport(0, 0, this.resolution, this.resolution);
        GL11.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void endWrite() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void copyDepthPrimaryToSecondary() {
        if (this.framebufferId == 0 || this.resolution <= 0 || this.depthTextures[1] == 0) {
            return;
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferId);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.depthTextures[1]);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, this.resolution, this.resolution);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public int getDepthTexture(int index) {
        return this.depthTextures[Math.max(0, Math.min(index, this.depthTextures.length - 1))];
    }

    public void delete() {
        for (int i = 0; i < this.depthTextures.length; i++) {
            if (this.depthTextures[i] != 0) {
                GL11.glDeleteTextures(this.depthTextures[i]);
                this.depthTextures[i] = 0;
            }
        }

        if (this.colorTexture != 0) {
            GL11.glDeleteTextures(this.colorTexture);
            this.colorTexture = 0;
        }

        if (this.framebufferId != 0) {
            GL30.glDeleteFramebuffers(this.framebufferId);
            this.framebufferId = 0;
        }

        this.resolution = 0;
        this.hardwareFiltering = null;
    }

    private static int createDepthTexture(int resolution) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL14.GL_COMPARE_R_TO_TEXTURE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_FUNC, GL11.GL_LEQUAL);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, resolution, resolution, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0L);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    private static int createColorTexture(int resolution) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, resolution, resolution, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }
}
