package com.dhj.actinium.mixin.vintage.core;

import net.coderbot.iris.debug.flight.GlFlightRecording;
import net.coderbot.iris.debug.flight.GlFlightStreamingSource;
import com.dhj.actinium.compat.dh.DistantHorizonsCompat;
import com.dhj.actinium.gui.ActiniumWindowModeController;
import com.dhj.actinium.render.BufferBuilderStreamingDrawer;
import com.dhj.actinium.render.EndPortalCompositeRenderer;
import com.dhj.actinium.runtime.ActiniumRuntime;
import com.gtnewhorizons.angelica.config.SystemProperties;
import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import com.gtnewhorizons.angelica.glsm.streaming.TessellatorStreamingDrawer;
import com.gtnewhorizons.angelica.sdlgpu.SDLGPUDisplayBridge;
import com.gtnewhorizons.angelica.sdlgpu.SDLGPUGate;
import com.mitchej123.lwjgl.LWJGLServiceProvider;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import org.embeddedt.embeddium.impl.render.frame.RenderAheadManager;
import org.lwjgl.glfw.GLFW;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Unique
    private final RenderAheadManager celeritas$renderAheadManager = new RenderAheadManager();

    @Unique
    private static boolean actinium$gameLoopStarted;

    @Unique
    private static boolean actinium$vsyncPushed;

    @Unique
    private static boolean actinium$debugPanelOpenedForWorld;

    @Inject(method = "runTick", at = @At("HEAD"))
    private void actinium$autoOpenDebugPanel(CallbackInfo ci) {
        final net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft) (Object) this;
        // The SDL GPU path cannot deliver in-game keyboard/mouse input yet, so the player cannot
        // press F3 manually; open the debug panel once after each world finishes loading so the
        // render state (fps, chunk stats) stays observable while diagnosing the backend.
        final boolean worldLoaded = mc.world != null && mc.player != null;
        if (worldLoaded && !actinium$debugPanelOpenedForWorld) {
            mc.gameSettings.showDebugInfo = true;
        }
        actinium$debugPanelOpenedForWorld = worldLoaded;
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void actinium$prepareDistantHorizonsBindingsLate(CallbackInfo ci) {
        // DistantHorizonsCompat loads DH classes; keep it out of the classpath when DH is absent.
        if (Loader.isModLoaded("distanthorizons")) {
            DistantHorizonsCompat.ensureClientBindings();
        }
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void preRender(CallbackInfo ci) {
        ActiniumWindowModeController.synchronize((Minecraft) (Object) this);
    }

    @Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
    private void actinium$toggleFullscreenMode(CallbackInfo ci) {
        ActiniumWindowModeController.toggleFullscreen((Minecraft) (Object) this);
        ci.cancel();
    }

    @Inject(method = "getLimitFramerate", at = @At("HEAD"), cancellable = true)
    private void actinium$useLoadingScreenFramerateLimit(CallbackInfoReturnable<Integer> cir) {
        Minecraft minecraft = (Minecraft) (Object) this;
        // Cap the frame rate while no world is loaded (menu, world join, dimension load). During
        // the join handshake of the integrated server the client thread must not outrun the FML
        // NetworkDispatcher registration: handling SPacketJoinGame before the handshake completes
        // NPEs in NetHandlerPlayClient and the world never loads. The vanilla check additionally
        // requires currentScreen != null, but the instant between displayGuiScreen(null) and world
        // assignment has currentScreen == null and is exactly where the race fires.
        if (minecraft.world == null) {
            cir.setReturnValue(ActiniumRuntime.options().performance.loadingScreenFramerateLimit);
        }
    }

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void beginRenderFrame(CallbackInfo ci) {
        GlFlightRecording.beginFrame();
        EndPortalCompositeRenderer.beginFrame();
        // Bootstrap the first frame: notify the active render backend (SDL GPU) that a frame
        // group starts, mirroring Angelica's MixinMinecraft_FrameHook.
        if (!actinium$gameLoopStarted) {
            actinium$gameLoopStarted = true;
            BackendManager.RENDER_BACKEND.onFrameBegin();
        }
        // The SDL GPU backend cannot rely on lwjglxx's Display.setVSyncEnabled (it only calls
        // glfwSwapInterval, which is a no-op on a GLFW_NO_API window): push the preference into
        // the backend once, so SDL's swapchain present mode is configured and the frame rate is
        // bounded like the GL path. Without it the client thread can outrun the FML handshake
        // (NetworkDispatcher registration) and crash on SPacketJoinGame during world join.
        final Minecraft mc = (Minecraft) (Object) this;
        if (SDLGPUGate.isActive() && !actinium$vsyncPushed && mc.gameSettings != null) {
            actinium$vsyncPushed = true;
            BackendManager.RENDER_BACKEND.setVSyncEnabled(mc.gameSettings.enableVsync);
        }
        final int limit = supportsCpuRenderAhead() ? ActiniumRuntime.options().advanced.cpuRenderAheadLimit : 0;
        if (limit > 0) {
            celeritas$renderAheadManager.startFrame(limit);
        }
    }

    @Inject(
            method = "runGameLoop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;updateDisplay()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void endStreamingFrame(CallbackInfo ci) {
        if (supportsCpuRenderAhead() && ActiniumRuntime.options().advanced.cpuRenderAheadLimit > 0) {
            celeritas$renderAheadManager.endFrame();
        }
        GlFlightRecording.beginStreamingSync(GlFlightStreamingSource.TESSELLATOR);
        TessellatorStreamingDrawer.endFrame();
        GlFlightRecording.endStreamingSync(GlFlightStreamingSource.TESSELLATOR);
        GlFlightRecording.beginStreamingSync(GlFlightStreamingSource.BUFFER_BUILDER);
        BufferBuilderStreamingDrawer.endFrame();
        GlFlightRecording.endStreamingSync(GlFlightStreamingSource.BUFFER_BUILDER);
        GlFlightRecording.beginSwap();
        // Frame boundary: notify the active render backend that the frame's draws are complete.
        BackendManager.RENDER_BACKEND.onFrameEnd();
    }

    @Inject(
            method = "runGameLoop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;updateDisplay()V",
                    shift = At.Shift.AFTER
            )
    )
    private void actinium$finishDisplaySwap(CallbackInfo ci) {
        GlFlightRecording.endSwap();
        // Frame boundary: the swap (or SDL present) is done; a new frame's draws may begin.
        if (actinium$gameLoopStarted) {
            BackendManager.RENDER_BACKEND.onFrameBegin();
        }
    }

    @Inject(method = "runGameLoop", at = @At("RETURN"))
    private void actinium$finishFlightRecorderFrame(CallbackInfo ci) {
        GlFlightRecording.endFrame();
    }

    /**
     * The SDL GPU backend presents through SDL, so the vanilla Display.update swap path (which
     * would swap a GL context on a GLFW_NO_API window) is replaced by event polling. Presentation
     * itself is driven by the frame hook: onFrameEnd (before this method) submits the final
     * target, and onFrameBegin (after this method) starts the next frame.
     */
    @Inject(method = "updateDisplay", at = @At("HEAD"), cancellable = true)
    private void actinium$sdlUpdateDisplay(CallbackInfo ci) {
        if (SDLGPUGate.isActive()) {
            GLFW.glfwPollEvents();
            SDLGPUDisplayBridge.pumpEvents();
            SDLGPUDisplayBridge.flushPendingMove();
            // lwjglxx drains Mouse/Keyboard inside Display.update(); the SDL path replaces that
            // method body, so poll here or the input queues never drain and the game sees no input.
            Mouse.poll();
            Keyboard.poll();
            ci.cancel();
        }
    }

    @Inject(method = "shutdownMinecraftApplet", at = @At("RETURN"))
    private void actinium$closeFlightRecorder(CallbackInfo ci) {
        GlFlightRecording.closeNormally();
    }

    /**
     * The LWJGL2 compatibility shim's {@code Display.destroy()} NPEs on the SDL backend: its
     * {@code Display$Window.releaseCallbacks()} frees every GLFW callback field unconditionally,
     * and those fields are never populated on the SDL path. The shim classes are invisible to the
     * mixin universe, so redirect the call site instead. {@code SDLGPUGate.isActive()} already
     * reports false at shutdown (the backend is torn down first), so key off the static config.
     */
    @Redirect(
            method = "shutdownMinecraftApplet",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;destroy()V", remap = false),
            remap = false
    )
    private static void actinium$sdlDisplayDestroy() {
        if (SystemProperties.USE_SDL_GPU && SDLGPUGate.isSDLGPUAvailable()) {
            SDLGPUDisplayBridge.safeDisplayDestroy();
        } else {
            try {
                Class.forName("org.lwjgl.opengl.Display").getMethod("destroy").invoke(null);
            } catch (ReflectiveOperationException e) {
                org.apache.logging.log4j.LogManager.getLogger("SDL-INPUT").error("Failed to destroy display", e);
            }
        }
    }

    @Unique
    private static boolean supportsCpuRenderAhead() {
        return LWJGLServiceProvider.LWJGL.isOpenGLVersionSupported(3, 2);
    }
}

