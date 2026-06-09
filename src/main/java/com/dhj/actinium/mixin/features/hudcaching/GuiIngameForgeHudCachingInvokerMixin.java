package com.dhj.actinium.mixin.features.hudcaching;

import com.gtnewhorizons.angelica.mixins.interfaces.GuiIngameForgeAccessor;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiIngameForge.class)
public abstract class GuiIngameForgeHudCachingInvokerMixin implements GuiIngameForgeAccessor {
    @Invoker(remap = false)
    public abstract void callRenderCrosshairs(float partialTicks);

    @Invoker(remap = false)
    public abstract void callRenderHelmet(ScaledResolution res, float partialTicks);

    @Invoker(remap = false)
    public abstract void callRenderPortal(ScaledResolution res, float partialTicks);

    @Invoker(remap = false)
    public abstract void callBind(ResourceLocation res);
}
