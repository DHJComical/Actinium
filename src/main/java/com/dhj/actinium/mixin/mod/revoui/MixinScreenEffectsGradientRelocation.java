package com.dhj.actinium.mixin.mod.revoui;

import com.dhj.actinium.render.RevoScreenEffectsGradient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Moves Revo UI's full-screen gradient ahead of the HUD so it cannot darken HUD elements.
 */
@Pseudo
@Mixin(targets = "neofontrender.addons.effects.ScreenEffectsRenderer", remap = false)
public abstract class MixinScreenEffectsGradientRelocation {
    @Redirect(
        method = "afterGameOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lneofontrender/addons/effects/ScreenEffectsRenderer;drawGradient(IIF)V"
        ),
        remap = false
    )
    // Capture only actual gradient draws; frames without one must not refresh the pending value.
    private void actinium$deferGradient(int width, int height, float progress) {
        Minecraft minecraft = Minecraft.getMinecraft();
        RevoScreenEffectsGradient.defer(
            width,
            height,
            progress,
            minecraft.world,
            minecraft.currentScreen
        );
    }
}
