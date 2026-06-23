package com.dhj.actinium.mixin.vintage.features.options;

import net.minecraftforge.client.GuiIngameForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.dhj.actinium.runtime.ActiniumRuntime;

@Mixin(GuiIngameForge.class)
public class MixinGuiIngameForge {
    @Redirect(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isFancyGraphicsEnabled()Z"))
    private boolean celeritas$redirectVignette() {
        return ActiniumRuntime.options().quality.enableVignette;
    }
}