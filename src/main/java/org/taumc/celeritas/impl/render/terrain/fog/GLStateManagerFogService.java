package org.taumc.celeritas.impl.render.terrain.fog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkFogMode;
import org.taumc.celeritas.mixin.core.terrain.EntityRendererAccessor;
import org.taumc.celeritas.mixin.core.terrain.GlStateManagerAccessor;
import org.taumc.celeritas.mixin.core.terrain.GlStateManagerBooleanStateAccessor;
import org.taumc.celeritas.mixin.core.terrain.GlStateManagerFogStateAccessor;

public class GLStateManagerFogService implements FogService {
    private static GlStateManagerFogStateAccessor getFogState() {
        return (GlStateManagerFogStateAccessor) (Object) GlStateManagerAccessor.celeritas$getFogState();
    }

    @Override
    public float getFogEnd() {
        return getFogState().celeritas$getEnd();
    }

    @Override
    public float getFogStart() {
        return getFogState().celeritas$getStart();
    }

    @Override
    public float getFogDensity() {
        return getFogState().celeritas$getDensity();
    }

    @Override
    public int getFogShapeIndex() {
        return 0;
    }

    @Override
    public float getFogCutoff() {
        return getFogState().celeritas$getEnd();
    }

    @Override
    public float[] getFogColor() {
        EntityRenderer entityRenderer = Minecraft.getMinecraft().entityRenderer;
        EntityRendererAccessor accessor = (EntityRendererAccessor) entityRenderer;
        return new float[]{accessor.celeritas$getFogColorRed(), accessor.celeritas$getFogColorGreen(), accessor.celeritas$getFogColorBlue(), 1.0F};
    }

    @Override
    public ChunkFogMode getFogMode() {
        GlStateManagerFogStateAccessor fogState = getFogState();
        if (!((GlStateManagerBooleanStateAccessor) (Object) fogState.celeritas$getFog()).celeritas$getCurrentState()) {
            return ChunkFogMode.NONE;
        }
        return ChunkFogMode.fromGLMode(fogState.celeritas$getMode());
    }
}
