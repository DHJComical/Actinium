package com.dhj.actinium.hudcaching;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.hooks.GLSMConfig;
import com.gtnewhorizons.angelica.mixins.interfaces.GuiIngameAccessor;
import com.gtnewhorizons.angelica.mixins.interfaces.GuiIngameForgeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.client.GuiIngameForge;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.taumc.celeritas.CeleritasVintage;

public final class ActiniumHudCaching {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final int CACHE_FPS = 20;

    public static Framebuffer framebuffer;
    public static boolean renderingCacheOverride;
    public static boolean renderVignetteCaptured;
    public static boolean renderHelmetCaptured;
    public static float renderPortalCapturedTicks;
    public static boolean renderCrosshairsCaptured;

    private static boolean dirty = true;
    private static long nextHudRefresh;

    private ActiniumHudCaching() {
    }

    public static void renderCachedHud(EntityRenderer renderer, GuiIngame ingame, float partialTicks) {
        GLStateManager.disableLighting();
        GLStateManager.glActiveTexture(GL13.GL_TEXTURE1);
        GLStateManager.disableTexture();
        GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
        GLStateManager.enableTexture();

        if (!shouldUseCache()) {
            if (MC.currentScreen != null) {
                dirty = true;
            }
            ingame.renderGameOverlay(partialTicks);
            restoreGuiState();
            return;
        }

        if (System.currentTimeMillis() > nextHudRefresh) {
            dirty = true;
        }

        if (dirty) {
            dirty = false;
            nextHudRefresh = System.currentTimeMillis() + 1000L / CACHE_FPS;
            prepareFramebuffer();

            renderingCacheOverride = true;
            GLSMConfig.hudCacheOverride = true;
            try {
                ingame.renderGameOverlay(partialTicks);
            } finally {
                renderingCacheOverride = false;
                GLSMConfig.hudCacheOverride = false;
                MC.getFramebuffer().bindFramebuffer(false);
            }
        } else {
            renderer.setupOverlayRendering();
        }

        ScaledResolution resolution = new ScaledResolution(MC);
        GLStateManager.enableBlend();
        GLStateManager.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        renderCapturedLayers(ingame, resolution, partialTicks);
        renderCachedFramebuffer(resolution);
        restoreGuiState();
    }

    public static void fixGLStateBeforeRenderingCache() {
        GLStateManager.glDepthMask(true);
        GLStateManager.enableDepthTest();
        GLStateManager.enableAlphaTest();
        GLStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GLStateManager.disableBlend();
    }

    public static boolean shouldReturnEarly() {
        return renderingCacheOverride;
    }

    private static boolean shouldUseCache() {
        return CeleritasVintage.options().advanced.hudCaching
            && MC.currentScreen == null
            && MC.player != null
            && MC.world != null
            && OpenGlHelper.isFramebufferEnabled()
            && GLStateManager.isFramebufferEnabled();
    }

    private static void prepareFramebuffer() {
        if (framebuffer == null) {
            framebuffer = new Framebuffer(MC.displayWidth, MC.displayHeight, true);
            framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        } else if (framebuffer.framebufferWidth != MC.displayWidth || framebuffer.framebufferHeight != MC.displayHeight) {
            framebuffer.createBindFramebuffer(MC.displayWidth, MC.displayHeight);
            framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        }

        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(true);
    }

    private static void renderCapturedLayers(GuiIngame ingame, ScaledResolution resolution, float partialTicks) {
        GuiIngameAccessor gui = (GuiIngameAccessor) ingame;
        if (renderVignetteCaptured) {
            gui.callRenderVignette(MC.player.getBrightness(), resolution);
        } else {
            GLStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        }

        if (ingame instanceof GuiIngameForge) {
            GuiIngameForgeAccessor guiForge = (GuiIngameForgeAccessor) ingame;
            if (renderHelmetCaptured) {
                guiForge.callRenderHelmet(resolution, partialTicks);
            }
            if (renderPortalCapturedTicks > 0.0F) {
                guiForge.callRenderPortal(resolution, partialTicks);
            }
            if (renderCrosshairsCaptured) {
                GLStateManager.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                guiForge.callRenderCrosshairs(partialTicks);
            }
        } else {
            if (renderHelmetCaptured) {
                gui.callRenderPumpkinOverlay(resolution);
            }
            if (renderPortalCapturedTicks > 0.0F) {
                gui.callRenderPortal(renderPortalCapturedTicks, resolution);
            }
        }
    }

    private static void renderCachedFramebuffer(ScaledResolution resolution) {
        GLStateManager.enableBlend();
        GLStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GLStateManager.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GLStateManager.disableDepthTest();
        GLStateManager.glDepthMask(false);
        GLStateManager.enableTexture();

        framebuffer.bindFramebufferTexture();
        drawTexturedRect((float) resolution.getScaledWidth_double(), (float) resolution.getScaledHeight_double());
        framebuffer.unbindFramebufferTexture();

        GLStateManager.glDepthMask(true);
        GLStateManager.enableDepthTest();
    }

    private static void restoreGuiState() {
        GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
        GLStateManager.enableTexture();
        GLStateManager.enableAlphaTest();
        GLStateManager.enableDepthTest();
        GLStateManager.glDepthMask(true);
        GLStateManager.enableBlend();
        GLStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GLStateManager.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        MC.getTextureManager().bindTexture(Gui.ICONS);
    }

    private static void drawTexturedRect(float width, float height) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(0.0D, height, 0.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
        buffer.pos(width, height, 0.0D).tex(1.0D, 0.0D).color(255, 255, 255, 255).endVertex();
        buffer.pos(width, 0.0D, 0.0D).tex(1.0D, 1.0D).color(255, 255, 255, 255).endVertex();
        buffer.pos(0.0D, 0.0D, 0.0D).tex(0.0D, 1.0D).color(255, 255, 255, 255).endVertex();
        tessellator.draw();
    }
}
