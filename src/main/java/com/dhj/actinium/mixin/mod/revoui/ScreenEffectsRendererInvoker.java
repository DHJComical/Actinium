package com.dhj.actinium.mixin.mod.revoui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes Revo UI's original gradient draw implementation at the pre-HUD boundary.
 */
@Pseudo
@Mixin(targets = "neofontrender.addons.effects.ScreenEffectsRenderer", remap = false)
public interface ScreenEffectsRendererInvoker {
    /**
     * Invokes the private static gradient renderer without reflection.
     */
    @Invoker("drawGradient")
    static void actinium$drawGradient(int width, int height, float progress) {
        throw new AssertionError("ScreenEffectsRenderer invoker was not transformed");
    }
}
