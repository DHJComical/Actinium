package org.taumc.celeritas.impl.render.terrain.fog;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.states.FogState;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkFogMode;
import org.joml.Vector3d;

public class GLStateManagerFogService implements FogService {
    private final float[] fogColor = new float[4];

    @Override
    public float getFogEnd() {
        return GLStateManager.getFogState().getEnd();
    }

    @Override
    public float getFogStart() {
        return GLStateManager.getFogState().getStart();
    }

    @Override
    public float getFogDensity() {
        return GLStateManager.getFogState().getDensity();
    }

    @Override
    public int getFogShapeIndex() {
        return 0;
    }

    @Override
    public float getFogCutoff() {
        return getFogEnd();
    }

    @Override
    public float[] getFogColor() {
        FogState state = GLStateManager.getFogState();
        Vector3d color = state.getFogColor();
        this.fogColor[0] = (float) color.x;
        this.fogColor[1] = (float) color.y;
        this.fogColor[2] = (float) color.z;
        this.fogColor[3] = state.getFogAlpha();
        return this.fogColor;
    }

    @Override
    public ChunkFogMode getFogMode() {
        if (!GLStateManager.getFogMode().isEnabled()) {
            return ChunkFogMode.NONE;
        }
        return ChunkFogMode.fromGLMode(GLStateManager.getFogState().getFogMode());
    }
}
