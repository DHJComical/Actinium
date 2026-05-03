package org.taumc.celeritas.impl.render.terrain.fog;

import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.shader.pack.ActiniumShaderProperties;
import com.dhj.actinium.shadows.ActiniumShadowRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import com.dhj.actinium.shader.pipeline.ActiniumRenderPipeline;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkFogMode;

public class GLStateManagerFogService implements FogService {
    @Override
    public float getFogEnd() {
        return GlStateManager.fogState.end;
    }

    @Override
    public float getFogStart() {
        return GlStateManager.fogState.start;
    }

    @Override
    public float getFogDensity() {
        return GlStateManager.fogState.density;
    }

    @Override
    public int getFogShapeIndex() {
        return 0;
    }

    @Override
    public float getFogCutoff() {
        if (ActiniumShadowRenderingState.areShadowsCurrentlyBeingRendered() && ActiniumShaderPackManager.areShadersEnabled()) {
            ActiniumShaderProperties properties = ActiniumShaderPackManager.getActiveShaderProperties();
            if (properties.isShadowEnabled()) {
                Minecraft minecraft = Minecraft.getMinecraft();
                float shadowDistance = Math.max(16.0f, Math.min(
                        properties.getShadowDistance(),
                        minecraft.gameSettings.renderDistanceChunks * 16.0f
                ));
                float renderMultiplier = properties.getShadowDistanceRenderMul();
                if (renderMultiplier >= 0.0f) {
                    shadowDistance *= renderMultiplier;
                }
                return Math.max(16.0f, shadowDistance);
            }
        }

        return GlStateManager.fogState.end;
    }

    @Override
    public float[] getFogColor() {
        float[] fog = ActiniumRenderPipeline.INSTANCE.getFogColor();
        ActiniumRenderPipeline.INSTANCE.debugLogFogState("GLStateManagerFogService.getFogColor", "read");
        return fog;
    }

    @Override
    public ChunkFogMode getFogMode() {
        if (!GlStateManager.fogState.fog.currentState) {
            return ChunkFogMode.NONE;
        }
        return ChunkFogMode.fromGLMode(GlStateManager.fogState.mode);
    }
}
