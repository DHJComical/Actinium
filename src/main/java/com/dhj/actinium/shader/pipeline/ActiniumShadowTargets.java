package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.celeritas.ActiniumShaders;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import lombok.Getter;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;

final class ActiniumShadowTargets {
    private final int[] depthTextures = new int[2];
    private final int[] colorTextures = new int[2];
    private int framebufferId;
    private int copyReadFramebufferId;
    private int copyDrawFramebufferId;
    private int resolution;
    private Boolean hardwareFiltering;
    private final IntBuffer drawBuffers = BufferUtils.createIntBuffer(2);

    public void ensureSize(int resolution) {
        if (this.resolution == resolution && this.depthTextures[0] != 0) {
            return;
        }

        this.delete();
        this.resolution = resolution;
        this.hardwareFiltering = null;
        this.depthTextures[0] = createDepthTexture(resolution);
        this.depthTextures[1] = createDepthTexture(resolution);
        this.colorTextures[0] = createColorTexture(resolution);
        this.colorTextures[1] = createColorTexture(resolution);
        this.framebufferId = GL30.glGenFramebuffers();
        if (this.copyReadFramebufferId == 0) {
            this.copyReadFramebufferId = GL30.glGenFramebuffers();
        }
        if (this.copyDrawFramebufferId == 0) {
            this.copyDrawFramebufferId = GL30.glGenFramebuffers();
        }
        if (ActiniumShaderPackManager.isDebugEnabled()) {
            ActiniumShaders.logger().info(
                    "[DEBUG] Shadow targets resized resolution={} framebuffer={} copyRead={} copyDraw={} depth0={} depth1={} color={}",
                    resolution,
                    this.framebufferId,
                    this.copyReadFramebufferId,
                    this.copyDrawFramebufferId,
                    this.depthTextures[0],
                    this.depthTextures[1],
                    this.colorTextures[0],
                    this.colorTextures[1]
            );
        }
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

        for (int colorTexture : this.colorTextures) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 0, 0,
                    Math.clamp(mainFramebuffer.framebufferWidth, 1, this.resolution),
                    Math.clamp(mainFramebuffer.framebufferHeight, 1, this.resolution), 0);
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void clear() {
        if (this.framebufferId == 0 || this.resolution <= 0) {
            return;
        }

        this.bindFramebufferForWrite(new int[]{0, 1});
        GL11.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        this.copyDepthPrimaryToSecondary();
        if (ActiniumShaderPackManager.isDebugEnabled()) {
            ActiniumShaders.logger().info(
                    "[DEBUG] Shadow targets cleared framebuffer={} resolution={} depth0={} depth1={} color0={} color1={}",
                    this.framebufferId,
                    this.resolution,
                    this.depthTextures[0],
                    this.depthTextures[1],
                    this.colorTextures[0],
                    this.colorTextures[1]
            );
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void beginWrite() {
        this.beginWrite(new int[]{0});
    }

    public void beginWrite(int[] colorAttachments) {
        this.bindFramebufferForWrite(colorAttachments);
        GL11.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        if (ActiniumShaderPackManager.isDebugEnabled()) {
            ActiniumShaders.logger().info(
                    "[DEBUG] Shadow targets beginWrite framebuffer={} resolution={} depth0={} depth1={} color0={} color1={} drawBuffers={}",
                    this.framebufferId,
                    this.resolution,
                    this.depthTextures[0],
                    this.depthTextures[1],
                    this.colorTextures[0],
                    this.colorTextures[1],
                    java.util.Arrays.toString(colorAttachments)
            );
        }
    }

    public void endWrite() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void copyDepthPrimaryToSecondary() {
        if (this.framebufferId == 0 || this.resolution <= 0 || this.depthTextures[1] == 0) {
            return;
        }

        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        try {
            ensureCopyFramebuffers();

            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.copyReadFramebufferId);
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.depthTextures[0], 0);
            GL11.glReadBuffer(GL11.GL_NONE);

            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.copyDrawFramebufferId);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.depthTextures[1], 0);
            GL11.glDrawBuffer(GL11.GL_NONE);

            GL30.glBlitFramebuffer(0, 0, this.resolution, this.resolution, 0, 0, this.resolution, this.resolution, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);

            if (ActiniumShaderPackManager.isDebugEnabled()) {
                ActiniumShaders.logger().info(
                        "[DEBUG] Shadow depth copied primary->secondary framebuffer={} read={} draw={} depth0={} depth1={}",
                        this.framebufferId,
                        this.copyReadFramebufferId,
                        this.copyDrawFramebufferId,
                        this.depthTextures[0],
                        this.depthTextures[1]
                );
            }
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
        }
    }

    public int getDepthTexture(int index) {
        return this.depthTextures[Math.max(0, Math.min(index, this.depthTextures.length - 1))];
    }

    public int getColorTexture() {
        return this.getColorTexture(0);
    }

    public int getColorTexture(int index) {
        return this.colorTextures[Math.max(0, Math.min(index, this.colorTextures.length - 1))];
    }

    public void delete() {
        for (int i = 0; i < this.depthTextures.length; i++) {
            if (this.depthTextures[i] != 0) {
                GL11.glDeleteTextures(this.depthTextures[i]);
                this.depthTextures[i] = 0;
            }
        }

        for (int i = 0; i < this.colorTextures.length; i++) {
            if (this.colorTextures[i] != 0) {
                GL11.glDeleteTextures(this.colorTextures[i]);
                this.colorTextures[i] = 0;
            }
        }

        if (this.framebufferId != 0) {
            GL30.glDeleteFramebuffers(this.framebufferId);
            this.framebufferId = 0;
        }

        if (this.copyReadFramebufferId != 0) {
            GL30.glDeleteFramebuffers(this.copyReadFramebufferId);
            this.copyReadFramebufferId = 0;
        }

        if (this.copyDrawFramebufferId != 0) {
            GL30.glDeleteFramebuffers(this.copyDrawFramebufferId);
            this.copyDrawFramebufferId = 0;
        }

        this.resolution = 0;
        this.hardwareFiltering = null;
    }

    private void ensureCopyFramebuffers() {
        if (this.copyReadFramebufferId == 0) {
            this.copyReadFramebufferId = GL30.glGenFramebuffers();
        }

        if (this.copyDrawFramebufferId == 0) {
            this.copyDrawFramebufferId = GL30.glGenFramebuffers();
        }
    }

    private void bindFramebufferForWrite(int[] colorAttachments) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.depthTextures[0], 0);

        this.drawBuffers.clear();

        for (int i = 0; i < colorAttachments.length; i++) {
            int colorIndex = Math.max(0, Math.min(colorAttachments[i], this.colorTextures.length - 1));
            int attachment = GL30.GL_COLOR_ATTACHMENT0 + i;
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D, this.colorTextures[colorIndex], 0);
            this.drawBuffers.put(attachment);
        }

        for (int i = colorAttachments.length; i < this.colorTextures.length; i++) {
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + i, GL11.GL_TEXTURE_2D, 0, 0);
        }

        this.drawBuffers.flip();
        GL20.glDrawBuffers(this.drawBuffers);
        GL11.glViewport(0, 0, this.resolution, this.resolution);
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
