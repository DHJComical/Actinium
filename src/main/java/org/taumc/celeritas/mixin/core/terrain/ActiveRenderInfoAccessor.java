package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.client.renderer.ActiveRenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.FloatBuffer;

@Mixin(ActiveRenderInfo.class)
public interface ActiveRenderInfoAccessor {
    @Accessor("PROJECTION")
    static FloatBuffer getProjectionMatrix() {
        throw new AssertionError();
    }

    @Accessor("MODELVIEW")
    static FloatBuffer getModelViewMatrix() {
        throw new AssertionError();
    }

    @Accessor("PROJECTION")
    static void setProjectionMatrix(FloatBuffer matrix) {
        throw new AssertionError();
    }

    @Accessor("MODELVIEW")
    static void setModelViewMatrix(FloatBuffer matrix) {
        throw new AssertionError();
    }
}
