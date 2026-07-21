package com.dhj.actinium.mixin.mod.lumenized;

import com.dhj.actinium.compat.lumenized.LumenizedBloomStrategy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gregtech.client.utils.BloomEffectUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Enforces the effective bloom style at Lumenized's post-processing dispatch. */
@Mixin(value = BloomEffectUtil.class, remap = false)
public abstract class MixinBloomEffectUtil {
    @ModifyExpressionValue(
        method = "renderBloomBlockLayer(Lnet/minecraft/client/renderer/RenderGlobal;"
            + "Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
        at = @At(
            value = "FIELD",
            target = "Lgithub/kasuminova/lumenized/common/config/LumenizedConfig;bloomStyle:I",
            opcode = Opcodes.GETSTATIC,
            remap = false
        ),
        require = 1,
        expect = 1,
        remap = false
    )
    private static int actinium$useEffectiveBloomStyle(int requestedStyle) {
        return LumenizedBloomStrategy.effectiveStyle(
            requestedStyle,
            LumenizedBloomStrategy.isExperimentalUnrealAllowed()
        );
    }
}
