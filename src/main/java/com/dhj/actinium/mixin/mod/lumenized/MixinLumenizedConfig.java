package com.dhj.actinium.mixin.mod.lumenized;

import com.dhj.actinium.compat.lumenized.LumenizedBloomStrategy;
import github.kasuminova.lumenized.common.config.LumenizedConfig;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies and reports Actinium's Lumenized safe mode after each config sync. */
@Mixin(value = LumenizedConfig.class, remap = false)
public abstract class MixinLumenizedConfig {
    @Unique
    private static final Logger actinium$LOGGER = LogManager.getLogger("Actinium");
    @Unique
    private static boolean actinium$loggedPolicy;

    @Shadow
    public static int bloomStyle;
    @Shadow
    public static boolean hookDepthTexture;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void actinium$applyInitialSafeMode(CallbackInfo callbackInfo) {
        actinium$applySafeMode();
    }

    @Inject(
        method = "onConfigChanged(Lnet/minecraftforge/fml/client/event/ConfigChangedEvent$OnConfigChangedEvent;)V",
        at = @At("TAIL"),
        remap = false
    )
    private static void actinium$applyChangedSafeMode(
        ConfigChangedEvent.OnConfigChangedEvent event,
        CallbackInfo callbackInfo
    ) {
        if ("lumenized".equals(event.getModID())) {
            actinium$applySafeMode();
        }
    }

    @Unique
    private static void actinium$applySafeMode() {
        int requestedStyle = bloomStyle;
        boolean requestedDepthHook = hookDepthTexture;
        boolean allowExperimental = LumenizedBloomStrategy.isExperimentalUnrealAllowed();
        bloomStyle = LumenizedBloomStrategy.effectiveStyle(requestedStyle, allowExperimental);
        hookDepthTexture = LumenizedBloomStrategy.effectiveDepthTextureHook(requestedDepthHook, allowExperimental);

        if (!actinium$loggedPolicy) {
            actinium$loggedPolicy = true;
            if (allowExperimental) {
                actinium$LOGGER.warn(
                    "Lumenized safe mode is disabled by -D{}=true; preserving bloomStyle={} and "
                        + "hookDepthTexture={}. These GPU paths are experimental and may render incorrectly.",
                    LumenizedBloomStrategy.EXPERIMENTAL_UNREAL_PROPERTY,
                    requestedStyle,
                    requestedDepthHook
                );
            } else {
                actinium$LOGGER.info(
                    "Lumenized safe mode is active: effective bloomStyle=0 and hookDepthTexture=false. "
                        + "The configuration file is unchanged. Set -D{}=true to restore both experimental paths.",
                    LumenizedBloomStrategy.EXPERIMENTAL_UNREAL_PROPERTY
                );
            }
        }
    }
}
