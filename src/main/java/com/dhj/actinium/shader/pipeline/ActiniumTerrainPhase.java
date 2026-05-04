package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.celeritas.shader_overrides.ActiniumTerrainPass;
import com.dhj.actinium.shader.pack.ActiniumShaderPackResources;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import net.minecraft.client.shader.Framebuffer;
import org.jetbrains.annotations.Nullable;

final class ActiniumTerrainPhase {
    private static final boolean ENABLE_EXTERNAL_TERRAIN_REDIRECT = false;
    private static final boolean ENABLE_EXTERNAL_TRANSLUCENT_TERRAIN_REDIRECT = true;

    private ActiniumTerrainPhase() {
    }

    static boolean isTranslucentTerrainRedirectEnabled() {
        return ENABLE_EXTERNAL_TRANSLUCENT_TERRAIN_REDIRECT;
    }

    static boolean shouldUseExternalTerrainRedirect(TerrainRenderPass pass) {
        if (pass == null) {
            return false;
        }

        if (ENABLE_EXTERNAL_TERRAIN_REDIRECT) {
            return true;
        }

        return ENABLE_EXTERNAL_TRANSLUCENT_TERRAIN_REDIRECT && pass.isReverseOrder();
    }

    static int[] resolveDrawBuffers(@Nullable ActiniumShaderPackResources resources, TerrainRenderPass pass) {
        if (pass == null) {
            return new int[]{ActiniumPostTargets.TARGET_COLORTEX1};
        }

        if (resources != null) {
            ActiniumTerrainPass terrainPass = resolveTerrainProgram(pass);
            return resources.readProgramDrawBuffers(terrainPass);
        }

        return new int[]{ActiniumPostTargets.TARGET_COLORTEX1, ActiniumPostTargets.TARGET_GAUX4};
    }

    private static ActiniumTerrainPass resolveTerrainProgram(TerrainRenderPass pass) {
        String passName = pass.name();

        if (pass.isReverseOrder() || "translucent".equals(passName)) {
            return ActiniumTerrainPass.GBUFFER_TRANSLUCENT;
        }

        if ("cutout".equals(passName)) {
            return ActiniumTerrainPass.GBUFFER_CUTOUT;
        }

        if ("cutout_mipped".equals(passName)) {
            return ActiniumTerrainPass.GBUFFER_CUTOUT_MIPPED;
        }

        return ActiniumTerrainPass.GBUFFER_SOLID;
    }

    static boolean shouldLogPass(@Nullable TerrainRenderPass pass) {
        return pass != null && "translucent".equals(pass.name());
    }

    static void debugLogPass(ActiniumRenderPipeline pipeline, String stage, TerrainRenderPass pass, Framebuffer framebuffer) {
        if (!shouldLogPass(pass)) {
            return;
        }

        pipeline.debugLogTerrainPassState(stage, pass);
        pipeline.debugLogTerrainFramebufferState(stage, pass, framebuffer);
    }

    static void presentPassResult(ActiniumRenderPipeline pipeline, Framebuffer framebuffer, int[] drawBuffers) {
        pipeline.presentTerrainPassResult(framebuffer, drawBuffers);
    }
}
