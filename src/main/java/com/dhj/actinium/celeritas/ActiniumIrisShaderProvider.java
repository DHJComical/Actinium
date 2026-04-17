package com.dhj.actinium.celeritas;

import com.dhj.actinium.block_rendering.ActiniumBlockRenderingSettings;
import net.minecraft.block.Block;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.celeritas.shader_overrides.ActiniumChunkProgramOverrides;
import com.dhj.actinium.celeritas.vertices.ActiniumExtendedChunkVertexType;
import com.dhj.actinium.shadows.ActiniumShadowRenderingState;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ActiniumIrisShaderProvider implements ActiniumShaderProvider {
    private static final ActiniumExtendedChunkVertexType EXTENDED_VERTEX_TYPE = new ActiniumExtendedChunkVertexType();

    private final ActiniumChunkProgramOverrides localOverrides = new ActiniumChunkProgramOverrides();
    private RenderPassConfiguration<?> renderPassConfiguration;
    private boolean loggedLocalOverridePath;
    private int observedReloadVersion = -1;

    @Override
    public boolean isShadersEnabled() {
        this.syncReloadState();
        return ActiniumShaderPackManager.areShadersEnabled();
    }

    @Override
    public boolean isShadowPass() {
        return this.isShadersEnabled() && ActiniumShadowRenderingState.areShadowsCurrentlyBeingRendered();
    }

    @Override
    public boolean shouldUseFaceCulling() {
        return !this.isShadowPass();
    }

    @Override
    public @Nullable GlProgram<? extends ChunkShaderInterface> getShaderOverride(TerrainRenderPass pass) {
        this.syncReloadState();

        if (!this.isShadersEnabled() || this.renderPassConfiguration == null) {
            return null;
        }

        GlProgram<? extends ChunkShaderInterface> localOverride = this.localOverrides.getProgramOverride(pass, this.renderPassConfiguration, this.isShadowPass());

        if (localOverride != null) {
            if (!this.loggedLocalOverridePath) {
                this.loggedLocalOverridePath = true;
                ActiniumShaders.logger().info("Using bundled Actinium terrain override programs");
            }

            return localOverride;
        }

        return null;
    }

    @Override
    public ChunkVertexType getVertexType(ChunkVertexType defaultType) {
        this.syncReloadState();

        if (this.isShadersEnabled() && ActiniumBlockRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat()) {
            return EXTENDED_VERTEX_TYPE;
        }

        return defaultType;
    }

    @Override
    public void setRenderPassConfiguration(RenderPassConfiguration<?> configuration) {
        this.syncReloadState();

        if (this.renderPassConfiguration != configuration) {
            this.localOverrides.deleteShaders();
            this.loggedLocalOverridePath = false;
        }

        this.renderPassConfiguration = configuration;
    }

    @Override
    public @Nullable Map<Block, ActiniumBlockRenderLayer> getBlockTypeIds() {
        this.syncReloadState();
        return ActiniumBlockRenderingSettings.INSTANCE.getBlockTypeIds();
    }

    @Override
    public void deleteShaders() {
        this.localOverrides.deleteShaders();
        this.renderPassConfiguration = null;
        this.loggedLocalOverridePath = false;
        this.observedReloadVersion = ActiniumShaderPackManager.getReloadVersion();
    }

    private void syncReloadState() {
        int reloadVersion = ActiniumShaderPackManager.getReloadVersion();

        if (this.observedReloadVersion != reloadVersion) {
            this.localOverrides.deleteShaders();
            this.renderPassConfiguration = null;
            this.loggedLocalOverridePath = false;
            this.observedReloadVersion = reloadVersion;
        }
    }
}
