package org.taumc.celeritas.impl.render.terrain.fog;

import net.minecraft.client.renderer.GlStateManager;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkFogMode;

public class GLStateManagerFogService implements FogService {
    private static final float[] FOG_COLOR = new float[] {1.0F, 1.0F, 1.0F, 1.0F};

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
        return GlStateManager.fogState.end;
    }

    @Override
    public float[] getFogColor() {
        return FOG_COLOR;
    }

    @Override
    public ChunkFogMode getFogMode() {
        if (!GlStateManager.fogState.fog.currentState) {
            return ChunkFogMode.NONE;
        }
        return ChunkFogMode.fromGLMode(GlStateManager.fogState.mode);
    }
}
