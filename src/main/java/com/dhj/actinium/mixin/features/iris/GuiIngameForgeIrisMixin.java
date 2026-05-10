package com.dhj.actinium.mixin.features.iris;

import net.minecraftforge.client.GuiIngameForge;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiIngameForge.class, remap = false)
public class GuiIngameForgeIrisMixin {
    @Inject(
        method = "renderCrosshairs",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;bind(Lnet/minecraft/util/ResourceLocation;)V", shift = At.Shift.AFTER)
    )
    private void actinium$enableAlphaForCrosshairs(CallbackInfo ci) {
        GL11.glEnable(GL11.GL_ALPHA_TEST);
    }
}
