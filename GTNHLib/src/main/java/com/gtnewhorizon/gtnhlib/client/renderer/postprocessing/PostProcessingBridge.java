package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class PostProcessingBridge {
    private static volatile DepthTextureProvider depthTextureProvider;
    private static volatile Function<EntityRenderer, int[]> lightmapColorAccessor;
    private static volatile Function<EntityRenderer, DynamicTexture> lightmapTextureAccessor;
    private static volatile BiFunction<EntityLivingBase, Float, Float> nightVisionBrightnessInvoker;

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

    public static void setLightmapTextureAccessor(Function<EntityRenderer, DynamicTexture> accessor) {
        lightmapTextureAccessor = accessor;
    }

    public static DynamicTexture getLightmapTexture(EntityRenderer entityRenderer) {
        Function<EntityRenderer, DynamicTexture> accessor = lightmapTextureAccessor;
        return accessor != null ? accessor.apply(entityRenderer) : null;
    }

    public static void setNightVisionBrightnessInvoker(BiFunction<EntityLivingBase, Float, Float> invoker) {
        nightVisionBrightnessInvoker = invoker;
    }

    public static float getNightVisionBrightness(EntityLivingBase entity, float partialTicks) {
        BiFunction<EntityLivingBase, Float, Float> invoker = nightVisionBrightnessInvoker;
        return invoker != null ? invoker.apply(entity, partialTicks) : 0.0F;
    }
}
