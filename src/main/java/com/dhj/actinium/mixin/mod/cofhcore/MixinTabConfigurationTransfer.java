package com.dhj.actinium.mixin.mod.cofhcore;

import com.dhj.actinium.render.GuiGlStateBoundary;
import cofh.core.gui.element.tab.TabConfigurationTransfer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores the translucent GUI state after CoFH's configuration tab draws.
 *
 * <p>The tab disables blending after drawing its own panel icons, while the
 * parent continues drawing the remaining tabs in the same foreground pass.</p>
 */
@Mixin(value = TabConfigurationTransfer.class, remap = false)
public abstract class MixinTabConfigurationTransfer {
    @Inject(method = "drawForeground", at = @At("RETURN"), remap = false)
    private void actinium$restoreGuiTabState(CallbackInfo ci) {
        GuiGlStateBoundary.beginTranslucentLayer();
    }
}
