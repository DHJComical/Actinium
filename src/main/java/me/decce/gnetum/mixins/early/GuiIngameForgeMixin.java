package me.decce.gnetum.mixins.early;

import com.google.common.base.Throwables;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.decce.gnetum.ASMEventHandlerHelper;
import me.decce.gnetum.ElementType;
import me.decce.gnetum.FramebufferManager;
import me.decce.gnetum.Gnetum;
import me.decce.gnetum.compat.betterhud.BetterHudCompat;
import me.decce.gnetum.compat.scalingguis.ScalingGuisCompat;
import me.decce.gnetum.hud.HudManager;
import me.decce.gnetum.hud.SharedValues;
import me.decce.gnetum.mixins.early.compat.EventBusAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

import static net.minecraftforge.client.GuiIngameForge.left_height;
import static net.minecraftforge.client.GuiIngameForge.right_height;
import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL;

@Mixin(value = GuiIngameForge.class)
public class GuiIngameForgeMixin {
    @Unique
    private final Minecraft gnetum$mc = Minecraft.getMinecraft();
    @Shadow
    private ScaledResolution res;
    @Shadow
    private FontRenderer fontrenderer;
    @Shadow
    private RenderGameOverlayEvent eventParent;

    @Unique
    private int gnetum$lastLeftHeight = 39;
    @Unique
    private int gnetum$lastRightHeight = 39;
    @Unique
    private int gnetum$currentLeftHeight;
    @Unique
    private int gnetum$currentRightHeight;
    @Unique
    private boolean gnetum$postEvent(RenderGameOverlayEvent event, Predicate<String> test) {
        EventBusAccessor eventBusAccessor = (EventBusAccessor)MinecraftForge.EVENT_BUS;

        if (eventBusAccessor.isShutdown()) return false;

        IEventListener[] listeners = event.getListenerList().getListeners(eventBusAccessor.getBusID());
        int index = 0;
        try
        {
            for (; index < listeners.length; index++)
            {
                IEventListener listener = listeners[index];
                if (listener instanceof EventPriority) {
                    listener.invoke(event);
                    continue;
                }
                String modid = null;
                if (listener instanceof ASMEventHandler asm) {
                    modid = ASMEventHandlerHelper.tryGetModId(asm);
                }
                if (modid == null) modid = Gnetum.OTHER_MODS;
                Gnetum.currentElement = modid;
                if (event instanceof RenderGameOverlayEvent.Pre) {
                    Gnetum.currentElementType = ElementType.PRE;
                }
                else if (event instanceof RenderGameOverlayEvent.Post) {
                    Gnetum.currentElementType = ElementType.POST;
                }
                if (test.test(modid)) {
                    listener.invoke(event);
                }
            }
        }
        catch (Throwable throwable)
        {
            eventBusAccessor.getExceptionHandler().handleException(MinecraftForge.EVENT_BUS, event, listeners, index, throwable);
            Throwables.throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        }
        return event.isCancelable() && event.isCanceled();
    }

    @WrapOperation(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;pre(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$ElementType;)Z", remap = false, ordinal = 0))
    public boolean gnetum$renderGameOverlay(GuiIngameForge instance, RenderGameOverlayEvent.ElementType type, Operation<Boolean> original, @Local(argsOnly = true) float partialTicks) {
        if (!Gnetum.config.isEnabled() || Gnetum.rendering) {
            return original.call(instance, type);
        }

        Gnetum.getScaledResolution().update();
        eventParent = new RenderGameOverlayEvent(partialTicks, Gnetum.getScaledResolution());

        SharedValues.partialTicks = partialTicks;
        fontrenderer = gnetum$mc.fontRenderer;

        if (Gnetum.passManager.current == 1) {
            gnetum$currentLeftHeight = 39;
            gnetum$currentRightHeight = 39;
        }

        right_height = 39;
        left_height = 39;

        if (ScalingGuisCompat.modInstalled) {
            ScalingGuisCompat.switchToHudScale(Gnetum.getScaledResolution());
        }

        FramebufferManager.getInstance().ensureSize();
        boolean framebufferComplete = FramebufferManager.getInstance().isComplete();
        if (framebufferComplete) {
            gnetum$mc.profiler.startSection("uncached");
            Gnetum.renderingCanceled = gnetum$postEvent(new RenderGameOverlayEvent.Pre(eventParent, RenderGameOverlayEvent.ElementType.ALL), modid -> Gnetum.passManager.cachingDisabled(modid, ElementType.PRE));
            gnetum$renderVanillaHuds(id -> Gnetum.passManager.cachingDisabled(id));
            if (BetterHudCompat.isEnabled()) {
                BetterHudCompat.onRenderGameOverlays(new RenderGameOverlayEvent.Pre(eventParent, ALL), true);
            }
            gnetum$mc.profiler.endSection();
        }
        else {
            gnetum$lastLeftHeight = 39;
            gnetum$lastRightHeight = 39;
            gnetum$currentLeftHeight = 39;
            gnetum$currentRightHeight = 39;
        }

        Gnetum.passManager.begin();

        if (Gnetum.passManager.current > 0) {
            FramebufferManager.getInstance().bind();
            Gnetum.rendering = true;

            SharedValues.defaultBlendFunc(); // Fixes ClassicBar
            Gnetum.renderingCanceled = gnetum$postEvent(new RenderGameOverlayEvent.Pre(eventParent, ALL), modid -> Gnetum.passManager.shouldRender(modid, ElementType.PRE));

            if (Gnetum.passManager.current != 1) {
                left_height = gnetum$currentLeftHeight;
                right_height = gnetum$currentRightHeight;
            }

            gnetum$renderVanillaHuds(id -> Gnetum.passManager.shouldRender(id));
            gnetum$postEvent(new RenderGameOverlayEvent.Post(eventParent, RenderGameOverlayEvent.ElementType.ALL), modid -> Gnetum.passManager.shouldRender(modid, ElementType.POST));

            if (BetterHudCompat.isEnabled()) {
                BetterHudCompat.onRenderGameOverlays(new RenderGameOverlayEvent.Pre(eventParent, ALL), false);
            }

            gnetum$currentLeftHeight = left_height;
            gnetum$currentRightHeight = right_height;

            Gnetum.rendering = false;
            Gnetum.currentElement = null;
        }
        Gnetum.passManager.end();

        if (Gnetum.passManager.current != Gnetum.config.numberOfPasses) {
            left_height = gnetum$lastLeftHeight;
            right_height = gnetum$lastRightHeight;
        }

        Gnetum.passManager.nextPass();

        if (Gnetum.passManager.current == Gnetum.config.numberOfPasses) {
            gnetum$lastLeftHeight = left_height;
            gnetum$lastRightHeight = right_height;
        }

        FramebufferManager.getInstance().unbind();

        if (ScalingGuisCompat.modInstalled) {
            ScalingGuisCompat.restoreGameScale(Gnetum.getScaledResolution());
        }

        if (framebufferComplete) {
            FramebufferManager.getInstance().blit(res.getScaledWidth_double(), res.getScaledHeight_double());

            if (ScalingGuisCompat.modInstalled) {
                ScalingGuisCompat.switchToHudScale(Gnetum.getScaledResolution());
            }

            gnetum$mc.profiler.startSection("uncached");
            gnetum$postEvent(new RenderGameOverlayEvent.Post(eventParent, RenderGameOverlayEvent.ElementType.ALL), modid -> Gnetum.passManager.cachingDisabled(modid, ElementType.POST));

            if (ScalingGuisCompat.modInstalled) {
                ScalingGuisCompat.restoreGameScale(Gnetum.getScaledResolution());
            }

            gnetum$mc.profiler.endSection();
        }
        else {
            return original.call(instance, type);
        }

        return true;
    }

    @Redirect(method = "renderHUDText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OpenGlHelper;glBlendFunc(IIII)V"))
    private static void gnetum$redirectStateManager(int sFactorRGB, int dFactorRGB, int sfactorAlpha, int dfactorAlpha) {
        GlStateManager.tryBlendFuncSeparate(sFactorRGB, dFactorRGB, sfactorAlpha, dfactorAlpha);
    }

    @Unique
    private void gnetum$renderVanillaHuds(Predicate<String> check) {
        for (int i = 0; i < HudManager.huds.size(); i++) {
            var hud = HudManager.huds.get(i);
            String id = hud.id().toString();
            if (hud.isDummy() || check.test(id)) {
                if (Gnetum.rendering) {
                    Gnetum.currentElement = id;
                    Gnetum.currentElementType = ElementType.VANILLA;
                }
                hud.render();
            }
        }
    }
}
