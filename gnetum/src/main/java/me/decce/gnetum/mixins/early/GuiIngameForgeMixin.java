package me.decce.gnetum.mixins.early;

import com.google.common.base.Throwables;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.decce.gnetum.ASMEventHandlerHelper;
import me.decce.gnetum.ElementType;
import me.decce.gnetum.FramebufferManager;
import me.decce.gnetum.Gnetum;
import me.decce.gnetum.GnetumDebug;
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

        if (eventBusAccessor.isShutdown()) {
            GnetumDebug.log("event-skip phase={} type={} reason=bus-shutdown", gnetum$eventPhase(event), event.getType());
            return false;
        }

        IEventListener[] listeners = event.getListenerList().getListeners(eventBusAccessor.getBusID());
        int index = 0;
        int matched = 0;
        int invoked = 0;
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
                boolean matches = test.test(modid);
                if (matches) {
                    matched++;
                    GnetumDebug.log("event-listener phase={} type={} modid={} listener={}", gnetum$eventPhase(event), event.getType(), modid, listener.getClass().getName());
                    listener.invoke(event);
                    invoked++;
                }
            }
        }
        catch (Throwable throwable)
        {
            eventBusAccessor.getExceptionHandler().handleException(MinecraftForge.EVENT_BUS, event, listeners, index, throwable);
            Throwables.throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        }
        boolean canceled = event.isCancelable() && event.isCanceled();
        GnetumDebug.log("event-summary phase={} type={} listeners={} matched={} invoked={} cancelable={} canceled={}",
                gnetum$eventPhase(event), event.getType(), listeners.length, matched, invoked, event.isCancelable(), canceled);
        return canceled;
    }

    @WrapOperation(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;pre(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$ElementType;)Z", remap = false, ordinal = 0))
    public boolean gnetum$renderGameOverlay(GuiIngameForge instance, RenderGameOverlayEvent.ElementType type, Operation<Boolean> original, @Local(argsOnly = true) float partialTicks) {
        boolean canUseGnetum = Gnetum.config.isEnabled() && !Gnetum.rendering;
        GnetumDebug.beginOverlayFrame(canUseGnetum);
        GnetumDebug.log("overlay-entry type={} partialTicks={} runtime={}", type, partialTicks, GnetumDebug.describeRuntime());
        if (!canUseGnetum) {
            GnetumDebug.log("overlay-fallback type={} reason={} runtime={}", type, Gnetum.rendering ? "recursive-render" : "disabled-or-no-framebuffer", GnetumDebug.describeRuntime());
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
            GnetumDebug.log("scalingguis switch-to-hud-scale");
            ScalingGuisCompat.switchToHudScale(Gnetum.getScaledResolution());
        }

        FramebufferManager.getInstance().ensureSize();
        boolean framebufferComplete = FramebufferManager.getInstance().isComplete();
        GnetumDebug.log("after-ensure-size framebufferComplete={} fbo={} scaled={}x{} scaleFactor={} heights left={} right={} currentLeft={} currentRight={} lastLeft={} lastRight={}",
                framebufferComplete,
                FramebufferManager.getInstance().describe(),
                Gnetum.getScaledResolution().getScaledWidth(),
                Gnetum.getScaledResolution().getScaledHeight(),
                Gnetum.getScaledResolution().getScaleFactor(),
                left_height,
                right_height,
                gnetum$currentLeftHeight,
                gnetum$currentRightHeight,
                gnetum$lastLeftHeight,
                gnetum$lastRightHeight);
        gnetum$prepareOverlayRendering();
        GnetumDebug.logGlState("after-prepare-before-uncached-pre");
        if (framebufferComplete) {
            GnetumDebug.log("uncached-pre-begin pass={}", Gnetum.passManager.current);
            gnetum$mc.profiler.startSection("uncached");
            Gnetum.renderingCanceled = gnetum$postEvent(new RenderGameOverlayEvent.Pre(eventParent, RenderGameOverlayEvent.ElementType.ALL), modid -> Gnetum.passManager.cachingDisabled(modid, ElementType.PRE));
            GnetumDebug.log("uncached-pre-end renderingCanceled={}", Gnetum.renderingCanceled);
            gnetum$renderVanillaHuds(id -> Gnetum.passManager.cachingDisabled(id), "uncached");
            if (BetterHudCompat.isEnabled()) {
                GnetumDebug.log("betterhud uncached-pre");
                BetterHudCompat.onRenderGameOverlays(new RenderGameOverlayEvent.Pre(eventParent, ALL), true);
            }
            gnetum$mc.profiler.endSection();
        }
        else {
            gnetum$lastLeftHeight = 39;
            gnetum$lastRightHeight = 39;
            gnetum$currentLeftHeight = 39;
            gnetum$currentRightHeight = 39;
            GnetumDebug.log("framebuffer-incomplete-reset-heights");
        }

        Gnetum.passManager.begin();

        if (Gnetum.passManager.current > 0) {
            FramebufferManager.getInstance().bind();
            Gnetum.rendering = true;
            GnetumDebug.log("cached-pass-begin pass={} heights left={} right={}", Gnetum.passManager.current, left_height, right_height);

            gnetum$prepareOverlayRendering();
            GnetumDebug.logGlState("after-prepare-cached-pre");
            Gnetum.renderingCanceled = gnetum$postEvent(new RenderGameOverlayEvent.Pre(eventParent, ALL), modid -> Gnetum.passManager.shouldRender(modid, ElementType.PRE));
            GnetumDebug.log("cached-pre-end pass={} renderingCanceled={}", Gnetum.passManager.current, Gnetum.renderingCanceled);

            if (Gnetum.passManager.current != 1) {
                left_height = gnetum$currentLeftHeight;
                right_height = gnetum$currentRightHeight;
                GnetumDebug.log("restored-current-heights pass={} left={} right={}", Gnetum.passManager.current, left_height, right_height);
            }

            gnetum$renderVanillaHuds(id -> Gnetum.passManager.shouldRender(id), "cached");
            gnetum$postEvent(new RenderGameOverlayEvent.Post(eventParent, RenderGameOverlayEvent.ElementType.ALL), modid -> Gnetum.passManager.shouldRender(modid, ElementType.POST));
            GnetumDebug.log("cached-post-end pass={} heights left={} right={}", Gnetum.passManager.current, left_height, right_height);

            if (BetterHudCompat.isEnabled()) {
                GnetumDebug.log("betterhud cached-pre pass={}", Gnetum.passManager.current);
                BetterHudCompat.onRenderGameOverlays(new RenderGameOverlayEvent.Pre(eventParent, ALL), false);
            }

            gnetum$currentLeftHeight = left_height;
            gnetum$currentRightHeight = right_height;

            Gnetum.rendering = false;
            Gnetum.currentElement = null;
            GnetumDebug.log("cached-pass-finish pass={} currentLeft={} currentRight={}", Gnetum.passManager.current, gnetum$currentLeftHeight, gnetum$currentRightHeight);
        }
        Gnetum.passManager.end();

        if (Gnetum.passManager.current != Gnetum.config.numberOfPasses) {
            left_height = gnetum$lastLeftHeight;
            right_height = gnetum$lastRightHeight;
            GnetumDebug.log("restored-last-heights pass={} left={} right={}", Gnetum.passManager.current, left_height, right_height);
        }

        Gnetum.passManager.nextPass();

        if (Gnetum.passManager.current == Gnetum.config.numberOfPasses) {
            gnetum$lastLeftHeight = left_height;
            gnetum$lastRightHeight = right_height;
            GnetumDebug.log("saved-last-heights pass={} left={} right={}", Gnetum.passManager.current, gnetum$lastLeftHeight, gnetum$lastRightHeight);
        }

        FramebufferManager.getInstance().unbind();
        GnetumDebug.logGlState("after-unbind");

        if (ScalingGuisCompat.modInstalled) {
            GnetumDebug.log("scalingguis restore-game-scale-before-blit");
            ScalingGuisCompat.restoreGameScale(Gnetum.getScaledResolution());
        }

        if (framebufferComplete) {
            gnetum$prepareOverlayRendering();
            GnetumDebug.logGlState("before-blit");
            FramebufferManager.getInstance().blit(res.getScaledWidth_double(), res.getScaledHeight_double());
            GnetumDebug.logGlState("after-blit");

            if (ScalingGuisCompat.modInstalled) {
                GnetumDebug.log("scalingguis switch-to-hud-scale-before-uncached-post");
                ScalingGuisCompat.switchToHudScale(Gnetum.getScaledResolution());
            }

            gnetum$prepareOverlayRendering();
            GnetumDebug.logGlState("after-prepare-before-uncached-post");
            gnetum$mc.profiler.startSection("uncached");
            gnetum$postEvent(new RenderGameOverlayEvent.Post(eventParent, RenderGameOverlayEvent.ElementType.ALL), modid -> Gnetum.passManager.cachingDisabled(modid, ElementType.POST));
            GnetumDebug.log("uncached-post-end");

            if (ScalingGuisCompat.modInstalled) {
                GnetumDebug.log("scalingguis restore-game-scale-after-uncached-post");
                ScalingGuisCompat.restoreGameScale(Gnetum.getScaledResolution());
            }

            gnetum$mc.profiler.endSection();
        }
        else {
            GnetumDebug.log("overlay-fallback type={} reason=framebuffer-incomplete-after-pass", type);
            return original.call(instance, type);
        }

        GnetumDebug.log("overlay-return type={} handled=true nextPass={} complete={}", type, Gnetum.passManager.current, FramebufferManager.getInstance().isComplete());
        return true;
    }

    @Unique
    private void gnetum$prepareOverlayRendering() {
        gnetum$mc.entityRenderer.setupOverlayRendering();
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        SharedValues.defaultBlendFunc();
    }

    @Redirect(method = "renderHUDText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OpenGlHelper;glBlendFunc(IIII)V"))
    private static void gnetum$redirectStateManager(int sFactorRGB, int dFactorRGB, int sfactorAlpha, int dfactorAlpha) {
        GlStateManager.tryBlendFuncSeparate(sFactorRGB, dFactorRGB, sfactorAlpha, dfactorAlpha);
    }

    @Unique
    private String gnetum$eventPhase(RenderGameOverlayEvent event) {
        if (event instanceof RenderGameOverlayEvent.Pre) return "pre";
        if (event instanceof RenderGameOverlayEvent.Post) return "post";
        return "parent";
    }

    @Unique
    private void gnetum$renderVanillaHuds(Predicate<String> check, String stage) {
        for (int i = 0; i < HudManager.huds.size(); i++) {
            var hud = HudManager.huds.get(i);
            String id = hud.id().toString();
            boolean dummy = hud.isDummy();
            boolean shouldRender = dummy || check.test(id);
            if (shouldRender) {
                if (Gnetum.rendering) {
                    Gnetum.currentElement = id;
                    Gnetum.currentElementType = ElementType.VANILLA;
                }
                boolean rendered = hud.renderWithResult();
                GnetumDebug.log("vanilla-hud stage={} index={} id={} dummy={} decision=render rendered={} pass={} renderingCanceled={}",
                        stage, i, id, dummy, rendered, Gnetum.passManager.current, Gnetum.renderingCanceled);
            }
            else {
                GnetumDebug.log("vanilla-hud stage={} index={} id={} dummy={} decision=skip pass={} renderingCanceled={}",
                        stage, i, id, dummy, Gnetum.passManager.current, Gnetum.renderingCanceled);
            }
        }
    }
}
