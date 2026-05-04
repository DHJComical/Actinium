package com.dhj.actinium.mixin.features.render;

import com.dhj.actinium.shader.pipeline.ActiniumRenderPipeline;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;
import java.lang.reflect.Method;

@Mixin(EntityRenderer.class)
public class EntityRendererActiniumColorStateMixin {
    private static final FloatBuffer ACTINIUM_FOG_COLOR_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final Method ACTINIUM_GL_GET_FLOAT = findMethod();

    @Inject(method = "updateFogColor", at = @At("TAIL"))
    private void actinium$captureFogColor(float partialTicks, CallbackInfo ci) {
        EntityRenderer renderer = (EntityRenderer) (Object) this;
        ActiniumRenderPipeline.INSTANCE.captureFallbackFogColor(
                renderer.fogColorRed,
                renderer.fogColorGreen,
                renderer.fogColorBlue,
                "EntityRenderer.updateFogColor"
        );
        ActiniumRenderPipeline.INSTANCE.debugLogFogState("EntityRenderer.updateFogColor", "tail");
    }

    @Inject(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;clearColor(FFFF)V"))
    private void actinium$captureClearColorBeforeApply(float partialTicks, CallbackInfo ci) {
        EntityRenderer renderer = (EntityRenderer) (Object) this;
        ActiniumRenderPipeline.INSTANCE.captureClearColor(
                renderer.fogColorRed,
                renderer.fogColorGreen,
                renderer.fogColorBlue,
                0.0f
        );
        ActiniumRenderPipeline.INSTANCE.debugLogFogState("EntityRenderer.updateFogColor", "before-clearColor");
    }

    @Inject(method = "setupFog", at = @At("TAIL"))
    private void actinium$captureFogModeState(int startCoords, float partialTicks, CallbackInfo ci) {
        if (GlStateManager.fogState.fog.currentState) {
            ACTINIUM_FOG_COLOR_BUFFER.clear();
            invokeGlGetFloat(GL11.GL_FOG_COLOR, ACTINIUM_FOG_COLOR_BUFFER);
            ActiniumRenderPipeline.INSTANCE.captureGlFogColor(
                    ACTINIUM_FOG_COLOR_BUFFER.get(0),
                    ACTINIUM_FOG_COLOR_BUFFER.get(1),
                    ACTINIUM_FOG_COLOR_BUFFER.get(2)
            );
        }

        if (!GlStateManager.fogState.fog.currentState) {
            float[] clear = ActiniumRenderPipeline.INSTANCE.getClearColor();
            ActiniumRenderPipeline.INSTANCE.captureFallbackFogColor(clear[0], clear[1], clear[2], "EntityRenderer.setupFog.disabled");
        }
        ActiniumRenderPipeline.INSTANCE.debugLogFogState("EntityRenderer.setupFog", "tail");
    }

    private static void invokeGlGetFloat(int parameter, FloatBuffer buffer) {
        if (ACTINIUM_GL_GET_FLOAT == null) {
            return;
        }

        try {
            ACTINIUM_GL_GET_FLOAT.invoke(null, parameter, buffer);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method findMethod() {
        try {
            return GL11.class.getMethod("glGetFloat", int.class, FloatBuffer.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
