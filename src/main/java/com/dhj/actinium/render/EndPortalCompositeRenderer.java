package com.dhj.actinium.render;

import com.gtnewhorizons.angelica.client.rendering.GlUniformFloat2v;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.RenderSystem;
import com.gtnewhorizons.angelica.glsm.states.Color4;
import com.gtnewhorizons.angelica.glsm.states.ColorMask;
import com.gtnewhorizons.angelica.glsm.states.ViewportState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat3v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import org.embeddedt.embeddium.impl.render.shader.ShaderLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Precomposes near-distance portal layers outside shader-pack programs.
 */
public final class EndPortalCompositeRenderer {
    private static final Logger LOGGER = LogManager.getLogger("EndPortalCompositeRenderer");
    private static final ResourceLocation END_SKY_TEXTURE = new ResourceLocation("textures/environment/end_sky.png");
    private static final ResourceLocation END_PORTAL_TEXTURE = new ResourceLocation("textures/entity/end_portal.png");
    private static final int TEXTURE_SIZE = 1024;
    private static final Map<Integer, CacheEntry> CACHE = new HashMap<>();

    private static GlProgram<CompositeUniforms> program;
    private static int emptyVao;
    private static long frame;
    private static boolean unavailable;

    private EndPortalCompositeRenderer() {
    }

    /** Starts a new client frame for per-tier cache invalidation. */
    public static void beginFrame() {
        frame++;
    }

    /**
     * Returns the current precomposed texture, or zero when initialization failed and the caller must use its old path.
     */
    static int texture(List<EndPortalLayers.Layer> layers) {
        if (!EndPortalCompositeLogic.shouldPrecompose(true, layers.size()) || unavailable) {
            return 0;
        }

        try {
            initialize();
            CacheEntry entry = CACHE.computeIfAbsent(layers.size(), ignored -> createCacheEntry());
            if (EndPortalCompositeLogic.needsUpdate(entry.lastUpdatedFrame, frame)) {
                update(entry, layers);
                entry.lastUpdatedFrame = frame;
            }
            return entry.texture;
        } catch (RuntimeException exception) {
            unavailable = true;
            LOGGER.error("Disabling end portal precomposition after an OpenGL failure", exception);
            return 0;
        }
    }

    /** Releases compositor resources before the current OpenGL context is destroyed. */
    public static void destroy() {
        for (CacheEntry entry : CACHE.values()) {
            GLStateManager.glDeleteFramebuffers(entry.framebuffer);
            TextureUtil.deleteTexture(entry.texture);
        }
        CACHE.clear();
        if (emptyVao != 0) {
            GLStateManager.glDeleteVertexArrays(emptyVao);
            emptyVao = 0;
        }
        if (program != null) {
            program.delete();
            program = null;
        }
        unavailable = false;
    }

    private static void initialize() {
        if (program != null) {
            return;
        }

        int previousProgram = GLStateManager.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GlShader vertex = null;
        GlShader fragment = null;
        try {
            vertex = ShaderLoader.loadShader(
                ShaderType.VERTEX,
                "actinium:end_portal_composite.vert",
                ShaderConstants.EMPTY
            );
            fragment = ShaderLoader.loadShader(
                ShaderType.FRAGMENT,
                "actinium:end_portal_composite.frag",
                ShaderConstants.EMPTY
            );
            program = GlProgram.builder("actinium:end_portal_composite")
                .attachShader(vertex)
                .attachShader(fragment)
                .link(CompositeUniforms::new);
            program.bind();
            program.getInterface().endSky.setInt(0);
            program.getInterface().endPortal.setInt(1);
        } finally {
            GLStateManager.glUseProgram(previousProgram);
            if (vertex != null) {
                vertex.delete();
            }
            if (fragment != null) {
                fragment.delete();
            }
        }
        emptyVao = GLStateManager.glGenVertexArrays();
    }

    private static CacheEntry createCacheEntry() {
        SavedState saved = SavedState.capture();
        int texture = 0;
        int framebuffer = 0;
        boolean complete = false;
        try {
            GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
            texture = GLStateManager.glGenTextures();
            GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GLStateManager.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL30.GL_RGBA16F,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                0,
                GL11.GL_RGBA,
                GL11.GL_FLOAT,
                (ByteBuffer) null
            );
            GLStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GLStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GLStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GLStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            framebuffer = GLStateManager.glGenFramebuffers();
            GLStateManager.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GLStateManager.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D,
                texture,
                0
            );
            GLStateManager.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
            int status = GLStateManager.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException(
                    "End portal compositor FBO incomplete: 0x" + Integer.toHexString(status)
                );
            }
            complete = true;
            return new CacheEntry(texture, framebuffer);
        } finally {
            saved.restore();
            if (!complete) {
                if (framebuffer != 0) {
                    GLStateManager.glDeleteFramebuffers(framebuffer);
                }
                if (texture != 0) {
                    TextureUtil.deleteTexture(texture);
                }
            }
        }
    }

    private static void update(CacheEntry entry, List<EndPortalLayers.Layer> layers) {
        SavedState saved = SavedState.capture();
        try {
            TextureManager textures = Minecraft.getMinecraft().getTextureManager();
            GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
            textures.bindTexture(END_SKY_TEXTURE);
            int endSky = GLStateManager.getBoundTextureForServerState();
            textures.bindTexture(END_PORTAL_TEXTURE);
            int endPortal = GLStateManager.getBoundTextureForServerState();

            GLStateManager.glBindFramebuffer(GL30.GL_FRAMEBUFFER, entry.framebuffer);
            GLStateManager.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
            GLStateManager.glViewport(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
            GLStateManager.glColorMask(true, true, true, true);
            GLStateManager.glDepthMask(false);
            GLStateManager.disableDepthTest();
            GLStateManager.disableScissorTest();
            GLStateManager.disableCull();
            GLStateManager.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GLStateManager.glClear(GL11.GL_COLOR_BUFFER_BIT);

            GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
            GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, endSky);
            GLStateManager.glActiveTexture(GL13.GL_TEXTURE1);
            GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, endPortal);

            RenderSystem.disableBufferBlend(0);
            program.bind();
            CompositeUniforms uniforms = program.getInterface();
            uniforms.layerCount.setInt(layers.size());
            for (int index = 0; index < layers.size(); index++) {
                EndPortalLayers.Layer layer = layers.get(index);
                uniforms.transforms[index].set(
                    layer.transformUU(),
                    layer.transformUV(),
                    layer.transformVU(),
                    layer.transformVV()
                );
                uniforms.offsets[index].set(layer.offsetU(), layer.offsetV());
                uniforms.colors[index].set(layer.red(), layer.green(), layer.blue());
            }

            GLStateManager.glBindVertexArray(emptyVao);
            GLStateManager.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        } finally {
            saved.restore();
        }
    }

    private static final class CompositeUniforms {
        private final GlUniformInt endSky;
        private final GlUniformInt endPortal;
        private final GlUniformInt layerCount;
        private final GlUniformFloat4v[] transforms = new GlUniformFloat4v[16];
        private final GlUniformFloat2v[] offsets = new GlUniformFloat2v[16];
        private final GlUniformFloat3v[] colors = new GlUniformFloat3v[16];

        private CompositeUniforms(ShaderBindingContext context) {
            this.endSky = context.bindUniform("u_EndSky", GlUniformInt::new);
            this.endPortal = context.bindUniform("u_EndPortal", GlUniformInt::new);
            this.layerCount = context.bindUniform("u_LayerCount", GlUniformInt::new);
            for (int index = 0; index < 16; index++) {
                this.transforms[index] = context.bindUniform("u_Transforms[" + index + "]", GlUniformFloat4v::new);
                this.offsets[index] = context.bindUniform("u_Offsets[" + index + "]", GlUniformFloat2v::new);
                this.colors[index] = context.bindUniform("u_Colors[" + index + "]", GlUniformFloat3v::new);
            }
        }
    }

    private static final class CacheEntry {
        private final int texture;
        private final int framebuffer;
        private long lastUpdatedFrame = -1L;

        private CacheEntry(int texture, int framebuffer) {
            this.texture = texture;
            this.framebuffer = framebuffer;
        }
    }

    private record SavedState(
        int program,
        int readFramebuffer,
        int drawFramebuffer,
        int[] drawBuffers,
        ViewportState viewport,
        int vao,
        int vbo,
        int activeTexture,
        int texture0,
        int texture1,
        ColorMask colorMask,
        boolean depthMask,
        boolean depthTest,
        boolean scissorTest,
        boolean cull,
        boolean bufferZeroBlend,
        Color4 clearColor
    ) {
        private static SavedState capture() {
            int maxDrawBuffers = GLStateManager.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
            int[] drawBuffers = new int[maxDrawBuffers];
            for (int index = 0; index < maxDrawBuffers; index++) {
                drawBuffers[index] = GLStateManager.glGetInteger(GL30.GL_DRAW_BUFFER0 + index);
            }
            return new SavedState(
                GLStateManager.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                GLStateManager.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
                GLStateManager.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                drawBuffers,
                GLStateManager.getViewportState().copy(),
                GLStateManager.getBoundVAO(),
                GLStateManager.getBoundVBO(),
                GLStateManager.getActiveTextureUnit(),
                GLStateManager.getBoundTextureForServerState(0),
                GLStateManager.getBoundTextureForServerState(1),
                GLStateManager.getColorMask().copy(),
                GLStateManager.getDepthState().isEnabled(),
                GLStateManager.glIsEnabled(GL11.GL_DEPTH_TEST),
                GLStateManager.glIsEnabled(GL11.GL_SCISSOR_TEST),
                GLStateManager.glIsEnabled(GL11.GL_CULL_FACE),
                GL30.glIsEnabledi(GL11.GL_BLEND, 0),
                GLStateManager.getClearColor().copy()
            );
        }

        private void restore() {
            GLStateManager.glUseProgram(this.program);
            GLStateManager.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.readFramebuffer);
            GLStateManager.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
            IntBuffer drawBufferValues = BufferUtils.createIntBuffer(this.drawBuffers.length);
            drawBufferValues.put(this.drawBuffers).flip();
            GLStateManager.glDrawBuffers(drawBufferValues);
            GLStateManager.glViewport(this.viewport.x, this.viewport.y, this.viewport.width, this.viewport.height);
            GLStateManager.glBindVertexArray(this.vao);
            GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
            GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
            GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, this.texture0);
            GLStateManager.glActiveTexture(GL13.GL_TEXTURE1);
            GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, this.texture1);
            GLStateManager.glActiveTexture(GL13.GL_TEXTURE0 + this.activeTexture);
            GLStateManager.glColorMask(
                this.colorMask.red,
                this.colorMask.green,
                this.colorMask.blue,
                this.colorMask.alpha
            );
            GLStateManager.glDepthMask(this.depthMask);
            restoreCapability(GL11.GL_DEPTH_TEST, this.depthTest);
            restoreCapability(GL11.GL_SCISSOR_TEST, this.scissorTest);
            restoreCapability(GL11.GL_CULL_FACE, this.cull);
            if (this.bufferZeroBlend) {
                RenderSystem.enableBufferBlend(0);
            } else {
                RenderSystem.disableBufferBlend(0);
            }
            GLStateManager.glClearColor(
                this.clearColor.getRed(),
                this.clearColor.getGreen(),
                this.clearColor.getBlue(),
                this.clearColor.getAlpha()
            );
        }

        private static void restoreCapability(int capability, boolean enabled) {
            if (enabled) {
                GLStateManager.glEnable(capability);
            } else {
                GLStateManager.glDisable(capability);
            }
        }
    }
}
