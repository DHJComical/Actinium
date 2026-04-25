package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.client.renderer.ActiveRenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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

    @Accessor("OBJECTCOORDS")
    static FloatBuffer getObjectCoords() {
        throw new AssertionError();
    }

    @Accessor("OBJECTCOORDS")
    static void setObjectCoords(FloatBuffer buffer) {
        throw new AssertionError();
    }

    @Accessor("VIEWPORT")
    static IntBuffer getViewportBuffer() {
        throw new AssertionError();
    }

    @Accessor("VIEWPORT")
    static void setViewportBuffer(IntBuffer buffer) {
        throw new AssertionError();
    }
}
