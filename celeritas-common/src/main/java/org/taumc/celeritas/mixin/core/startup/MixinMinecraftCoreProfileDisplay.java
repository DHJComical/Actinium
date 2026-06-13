package org.taumc.celeritas.mixin.core.startup;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ForgeHooksClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftCoreProfileDisplay {
    @Unique
    private static final Logger celeritas$LOGGER = LogManager.getLogger("Celeritas");

    @Shadow
    private boolean fullscreen;

    @Inject(method = "createDisplay", at = @At("HEAD"), cancellable = true)
    private void celeritas$createCoreProfileDisplay(CallbackInfo ci) throws LWJGLException {
        Display.setResizable(true);
        Display.setTitle("Cleanroom");

        PixelFormat format = new PixelFormat().withDepthBits(24).withStencilBits(8);
        int maxMajor = 4;
        int maxMinor = LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_MACOSX ? 1 : 6;
        Exception lastException = null;

        for (int major = maxMajor; major >= 3; --major) {
            int startMinor = major == 4 ? maxMinor : 3;
            int endMinor = major == 3 ? 3 : 0;

            for (int minor = startMinor; minor >= endMinor; --minor) {
                ContextAttribs attribs = celeritas$createContextAttribs(major, minor);
                try {
                    Display.create(format, attribs);
                    celeritas$LOGGER.info("Created OpenGL {}.{} core profile context", major, minor);
                    ForgeHooksClient.initializeWindowsInformation();
                    ForgeHooksClient.setWindowStyle(this.fullscreen);
                    ForgeHooksClient.initializeTaskbarAPI();
                    ci.cancel();
                    return;
                } catch (RuntimeException e) {
                    lastException = e;
                    try {
                        Display.destroy();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        throw new LWJGLException("Failed to create an OpenGL 3.3+ core profile context", lastException);
    }

    @Unique
    private static ContextAttribs celeritas$createContextAttribs(int major, int minor) {
        ContextAttribs attribs = new ContextAttribs(major, minor);
        attribs.withProfileCore(true);
        attribs.withForwardCompatible(true);
        attribs.withDebug(Boolean.getBoolean("actinium.lwjglDebug"));
        return attribs;
    }
}
