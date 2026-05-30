package com.gtnewhorizons.angelica.mixins.interfaces;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

public interface GuiIngameForgeAccessor {
    void callRenderCrosshairs(float partialTicks);

    void callRenderHelmet(ScaledResolution res, float partialTicks);

    void callRenderPortal(ScaledResolution res, float partialTicks);

    void callBind(ResourceLocation res);
}
