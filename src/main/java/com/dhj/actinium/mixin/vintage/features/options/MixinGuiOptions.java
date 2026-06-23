package com.dhj.actinium.mixin.vintage.features.options;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.dhj.actinium.gui.ActiniumVideoOptionsScreen;

@Mixin(GuiOptions.class)
public class MixinGuiOptions extends GuiScreen {

    @Dynamic
    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void open(GuiButton button, CallbackInfo ci) {
        if(button.enabled && button.id == 101) {
            this.mc.displayGuiScreen(new ActiniumVideoOptionsScreen(this));

            ci.cancel();
        }
    }
}
