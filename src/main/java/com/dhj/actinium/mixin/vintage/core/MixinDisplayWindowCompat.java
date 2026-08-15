package com.dhj.actinium.mixin.vintage.core;

import com.gtnewhorizons.angelica.config.SystemProperties;
import com.gtnewhorizons.angelica.sdlgpu.SDLGPUGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips {@code org.lwjglx.opengl.Display$Window.releaseCallbacks()} on the SDL GPU backend.
 *
 * <p>lwjglxx releases all twelve GLFW callback fields unconditionally (no null check). The SDL
 * backend installs only the callbacks it needs (cursor/button/scroll/key/char/focus); the
 * remaining fields stay null, so the shutdown path would NPE inside {@code free()}. There is
 * nothing to release on the SDL window, and GLFW releases the callbacks when the window is
 * destroyed anyway.</p>
 *
 * <p>{@code SDLGPUGate.isActive()} cannot be used here: the SDL backend is torn down before
 * {@code Display.destroy()} runs during shutdown, so it already reports false when this method
 * executes. The static configuration is process-wide and stable, which is all this guard needs.</p>
 */
@Mixin(targets = "org/lwjglx/opengl/Display$Window", remap = false)
public abstract class MixinDisplayWindowCompat {

    @Inject(method = "releaseCallbacks", at = @At("HEAD"), cancellable = true, remap = false)
    private static void actinium$sdlSkipReleaseCallbacks(CallbackInfo ci) {
        if (SystemProperties.USE_SDL_GPU && SDLGPUGate.isSDLGPUAvailable()) {
            ci.cancel();
        }
    }
}
