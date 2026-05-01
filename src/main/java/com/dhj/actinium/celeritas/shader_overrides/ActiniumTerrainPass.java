package com.dhj.actinium.celeritas.shader_overrides;

import net.minecraft.util.BlockRenderLayer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;

public enum ActiniumTerrainPass {
    SHADOW("shadow_solid"),
    SHADOW_CUTOUT("shadow_cutout"),
    GBUFFER_SOLID("gbuffers_terrain"),
    GBUFFER_CUTOUT_MIPPED("gbuffers_terrain_cutout_mip"),
    GBUFFER_CUTOUT("gbuffers_terrain_cutout"),
    GBUFFER_TRANSLUCENT("gbuffers_water");

    private final String name;

    ActiniumTerrainPass(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public TerrainRenderPass toTerrainPass(RenderPassConfiguration<?> configuration) {
        return switch (this) {
            case SHADOW, GBUFFER_SOLID -> configuration.defaultSolidMaterial().pass;
            case SHADOW_CUTOUT, GBUFFER_CUTOUT_MIPPED -> configuration.getMaterialForRenderType(BlockRenderLayer.CUTOUT_MIPPED).pass;
            case GBUFFER_CUTOUT -> configuration.getMaterialForRenderType(BlockRenderLayer.CUTOUT).pass;
            case GBUFFER_TRANSLUCENT -> configuration.defaultTranslucentMaterial().pass;
        };
    }

    public static ActiniumTerrainPass fromTerrainPass(TerrainRenderPass pass, RenderPassConfiguration<?> configuration, boolean shadowPass) {
        if (shadowPass) {
            return pass.supportsFragmentDiscard() ? SHADOW_CUTOUT : SHADOW;
        }

        if (pass == configuration.getMaterialForRenderType(BlockRenderLayer.CUTOUT_MIPPED).pass) {
            return GBUFFER_CUTOUT_MIPPED;
        }

        if (pass == configuration.getMaterialForRenderType(BlockRenderLayer.CUTOUT).pass) {
            return GBUFFER_CUTOUT;
        }

        if (pass.supportsFragmentDiscard()) {
            return GBUFFER_CUTOUT;
        }

        return pass.isReverseOrder() ? GBUFFER_TRANSLUCENT : GBUFFER_SOLID;
    }
}
