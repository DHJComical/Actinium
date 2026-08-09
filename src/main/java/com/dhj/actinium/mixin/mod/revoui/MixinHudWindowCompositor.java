package com.dhj.actinium.mixin.mod.revoui;

import com.dhj.actinium.render.GuiGlStateBoundary;
import neofontrender.addons.hud.compositor.HudSurface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "neofontrender.addons.hud.compositor.HudWindowCompositor", remap = false)
public abstract class MixinHudWindowCompositor {
    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void actinium$restoreCompositorEntryState(float partialTicks, CallbackInfo ci) {
        GuiGlStateBoundary.restoreHudBaseline();
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void actinium$restoreCompositorExitState(float partialTicks, CallbackInfo ci) {
        GuiGlStateBoundary.restoreHudBaseline();
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lneofontrender/addons/hud/compositor/HudSurface;render(F)V"
        ),
        remap = false
    )
    // Isolate the state Revo surfaces mutate while leaving compositor-owned scissor state untouched.
    private void actinium$renderSurfaceWithStateBoundary(HudSurface surface, float partialTicks) {
        GuiGlStateBoundary.CompositorSurfaceState previous = GuiGlStateBoundary.beginCompositorSurface();
        try {
            surface.render(partialTicks);
        } finally {
            previous.restore();
            GuiGlStateBoundary.restoreHudBaseline();
        }
    }

}
