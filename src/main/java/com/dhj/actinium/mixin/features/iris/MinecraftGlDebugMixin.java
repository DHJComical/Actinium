package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.debug.IrisGlDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MinecraftGlDebugMixin {
    @Redirect(method = "checkGLError(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;glGetError()I"))
    private int actinium$logGlStateForErrors(String message) {
        int error = GlStateManager.glGetError();
        IrisGlDebug.logMinecraftGlError(message, error);
        return error;
    }
}
