package com.dhj.actinium.mixin.mod.lumenized;

import com.dhj.actinium.compat.lumenized.LumenizedBloomStrategy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gregtech.client.utils.DepthTextureUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Enforces the effective depth hook option before Lumenized enters its FBO path. */
@Mixin(value = DepthTextureUtil.class, remap = false)
public abstract class MixinDepthTextureUtil {
    @ModifyExpressionValue(
        method = "shouldRenderDepthTexture()Z",
        at = @At(
            value = "FIELD",
            target = "Lgithub/kasuminova/lumenized/common/config/LumenizedConfig;hookDepthTexture:Z",
            opcode = Opcodes.GETSTATIC,
            remap = false
        ),
        require = 1,
        expect = 1,
        remap = false
    )
    private static boolean actinium$useEffectiveDepthTextureHook(boolean requestedHook) {
        return LumenizedBloomStrategy.effectiveDepthTextureHook(
            requestedHook,
            LumenizedBloomStrategy.isExperimentalUnrealAllowed()
        );
    }
}
