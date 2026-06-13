package me.decce.gnetum.mixins.early;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiIngameForge.class)
public interface GuiIngameForgeAccessor {
    @Invoker
    boolean callPre(RenderGameOverlayEvent.ElementType type);
    @Invoker
    void callPost(RenderGameOverlayEvent.ElementType type);
    @Invoker
    void callRenderCrosshairs(float partialTicks);
    @Invoker
    void callRenderHelmet(ScaledResolution res, float partialTicks);
    @Invoker
    void callRenderPortal(ScaledResolution res, float partialTicks);
    @Invoker
    void callRenderSleepFade(int width, int height);
    @Invoker
    void callRenderSubtitles(ScaledResolution resolution);
    @Invoker
    void callRenderBossHealth();
    @Invoker
    void callRenderArmor(int width, int height);
    @Invoker
    void callRenderHUDText(int width, int height);
    @Invoker
    void callRenderExperience(int width, int height);
    @Invoker
    void callRenderHealth(int width, int height);
    @Invoker
    void callRenderToolHighlight(ScaledResolution res);
    @Invoker
    void callRenderJumpBar(int width, int height);
    @Invoker
    void callRenderFPSGraph();
    @Invoker
    void callRenderRecordOverlay(int width, int height, float partialTicks);
    @Invoker
    void callRenderPotionIcons(ScaledResolution resolution);
    @Invoker
    void callRenderChat(int width, int height);
    @Invoker
    void callRenderTitle(int width, int height, float partialTicks);
    @Invoker
    void callRenderFood(int width, int height);
    @Invoker
    void callRenderHealthMount(int width, int height);
    @Invoker
    void callRenderAir(int width, int height);
    @Invoker
    void callRenderPlayerList(int width, int height);
}