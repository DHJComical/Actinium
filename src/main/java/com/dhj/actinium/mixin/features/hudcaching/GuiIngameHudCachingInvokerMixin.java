package com.dhj.actinium.mixin.features.hudcaching;

import com.gtnewhorizons.angelica.mixins.interfaces.GuiIngameAccessor;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiIngame.class)
public abstract class GuiIngameHudCachingInvokerMixin implements GuiIngameAccessor {
    @Invoker
    public abstract void callRenderVignette(float lightLevel, ScaledResolution scaledRes);

    @Invoker
    public abstract void callRenderPumpkinOverlay(ScaledResolution scaledRes);

    @Invoker
    public abstract void callRenderPortal(float timeInPortal, ScaledResolution scaledRes);
}
