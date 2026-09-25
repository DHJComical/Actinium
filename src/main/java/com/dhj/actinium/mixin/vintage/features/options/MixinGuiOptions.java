package com.dhj.actinium.mixin.vintage.features.options;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.dhj.actinium.compat.sodium.ActiniumOptionHost;

/**
 * Routes the vanilla video settings button (id 101) into Reese's Sodium
 * Options. When RSO is disabled the vanilla video settings screen is shown
 * instead, so the Sodium-derived settings UI is never opened.
 */
@Mixin(GuiOptions.class)
public class MixinGuiOptions extends GuiScreen {

    @Dynamic
    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void open(GuiButton button, CallbackInfo ci) {
        if (button.enabled && button.id == 101) {
            if (ReeseSodiumOptionsConfig.config().isEnabled()) {
                this.mc.displayGuiScreen(new SodiumVideoOptionsScreen(this,
                        ActiniumOptionHost.shared().modOptions()));
                ci.cancel();
            }
        }
    }
}
