package com.dhj.actinium.celeritas.shader_overrides;

import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;

public enum ActiniumTerrainPass {
    SHADOW("shadow"),
    SHADOW_CUTOUT("shadow"),
    GBUFFER_SOLID("gbuffers_terrain"),
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
            case SHADOW_CUTOUT, GBUFFER_CUTOUT -> configuration.defaultCutoutMippedMaterial().pass;
            case GBUFFER_TRANSLUCENT -> configuration.defaultTranslucentMaterial().pass;
        };
    }

    public static ActiniumTerrainPass fromTerrainPass(TerrainRenderPass pass, boolean shadowPass) {
        if (shadowPass) {
            return pass.supportsFragmentDiscard() ? SHADOW_CUTOUT : SHADOW;
        }

        if (pass.supportsFragmentDiscard()) {
            return GBUFFER_CUTOUT;
        }

        return pass.isReverseOrder() ? GBUFFER_TRANSLUCENT : GBUFFER_SOLID;
    }
}
