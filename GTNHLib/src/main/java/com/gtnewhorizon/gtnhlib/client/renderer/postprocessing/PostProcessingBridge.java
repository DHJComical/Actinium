package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.shader.Framebuffer;

import java.util.function.Function;

public final class PostProcessingBridge {
    private static volatile DepthTextureProvider depthTextureProvider;
    private static volatile Function<EntityRenderer, int[]> lightmapColorAccessor;

    private PostProcessingBridge() {
    }

    public static void setDepthTextureProvider(DepthTextureProvider provider) {
        depthTextureProvider = provider;
    }

    public static int getDepthTextureId(Framebuffer framebuffer) {
        if (depthTextureProvider == null) {
            throw new UnsupportedOperationException("No depth texture provider is registered.");
        }

        return depthTextureProvider.getDepthTextureId(framebuffer);
    }

    public static boolean hasDepthTextureProvider() {
        return depthTextureProvider != null;
    }

    public static void setLightmapColorAccessor(Function<EntityRenderer, int[]> accessor) {
        lightmapColorAccessor = accessor;
    }

    public static int[] getLightmapColors(EntityRenderer entityRenderer) {
        Function<EntityRenderer, int[]> accessor = lightmapColorAccessor;
        return accessor != null ? accessor.apply(entityRenderer) : null;
    }
}
