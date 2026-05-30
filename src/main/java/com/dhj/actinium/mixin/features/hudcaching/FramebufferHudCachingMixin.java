package com.dhj.actinium.mixin.features.hudcaching;

import com.dhj.actinium.hudcaching.ActiniumHudCaching;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Framebuffer.class)
public class FramebufferHudCachingMixin {
    @Inject(method = "bindFramebuffer", at = @At("HEAD"), cancellable = true)
    private void actinium$bindHudCacheFramebuffer(boolean viewport, CallbackInfo ci) {
        Framebuffer framebuffer = (Framebuffer) (Object) this;
        if (ActiniumHudCaching.renderingCacheOverride && framebuffer == Minecraft.getMinecraft().getFramebuffer()) {
            ActiniumHudCaching.framebuffer.bindFramebuffer(viewport);
            ci.cancel();
        }
    }
}
