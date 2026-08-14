package com.dhj.actinium.mixin.vintage.core;

import com.gtnewhorizons.angelica.sdlgpu.SDLGPUGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes the legacy LWJGL2 {@code Display.getWindow()} to the SDL backend's window.
 *
 * <p>Cleanroom's window integrations (DWM styling, taskbar progress) resolve the GLFW window
 * through {@code org.lwjgl.opengl.Display.getWindow()}. On the SDL GPU backend the window is
 * created directly (GLFW_NO_API) and mirrored into lwjglxx's {@code Display}, so this mixin makes
 * those integrations see the same window instead of handle 0.</p>
 */
@Mixin(targets = "org/lwjgl/opengl/Display", remap = false)
public abstract class MixinDisplayCompat {

    @Inject(method = "getWindow", at = @At("HEAD"), cancellable = true, remap = false)
    private static void actinium$sdlGetWindow(CallbackInfoReturnable<Long> cir) {
        if (SDLGPUGate.isActive()) {
            cir.setReturnValue(org.lwjglx.opengl.Display.getWindow());
        }
    }
}
