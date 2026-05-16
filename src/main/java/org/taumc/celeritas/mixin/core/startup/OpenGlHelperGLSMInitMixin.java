package org.taumc.celeritas.mixin.core.startup;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.hooks.GLSMHooks;
import com.gtnewhorizons.angelica.glsm.hooks.GLSMInitConfig;
import com.gtnewhorizons.angelica.glsm.recording.ImmediateModeRecorder;
import com.gtnewhorizons.angelica.glsm.streaming.TessellatorStreamingDrawer;
import net.coderbot.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OpenGlHelper.class, priority = 100)
public class OpenGlHelperGLSMInitMixin {
    @Inject(method = "initializeTextures", at = @At("RETURN"))
    private static void celeritas$initializeGLStateManager(CallbackInfo ci) {
        final Minecraft mc = Minecraft.getMinecraft();

        GLStateManager.setDrawableGL(Display.getDrawable());
        GLStateManager.initialize(GLSMInitConfig.builder()
            .displaySize(mc.displayWidth, mc.displayHeight)
            .framebufferSupported(OpenGlHelper.framebufferSupported)
            .fboEnabled(mc.gameSettings.fboEnable)
            .directDrawer(TessellatorStreamingDrawer::drawDirect)
            .streamingDrawerDestroy(TessellatorStreamingDrawer::destroy)
            .build());

        GLSMHooks.LIGHTMAP_COORDS.addListener(event -> {
            OpenGlHelper.lastBrightnessX = event.x;
            OpenGlHelper.lastBrightnessY = event.y;
        });

        if (Iris.enabled && Thread.currentThread() == GLStateManager.getMainThread()) {
            Iris.onRenderSystemInit();
        }
    }
}
