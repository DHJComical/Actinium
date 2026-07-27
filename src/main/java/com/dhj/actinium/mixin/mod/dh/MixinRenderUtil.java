package com.dhj.actinium.mixin.mod.dh;

import com.dhj.actinium.compat.dh.DistantHorizonsIrisAccessorState;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Selects the vanilla Distant Horizons far plane whenever no shader pack is active. */
@Mixin(value = RenderUtil.class, remap = false)
public abstract class MixinRenderUtil {
    @ModifyExpressionValue(
        method = "getFarClipPlaneDistanceInBlocks()F",
        at = @At(
            value = "FIELD",
            target = "Lcom/seibel/distanthorizons/core/util/RenderUtil;IRIS_ACCESSOR:"
                + "Lcom/seibel/distanthorizons/core/wrapperInterfaces/modAccessor/IIrisAccessor;",
            opcode = Opcodes.GETSTATIC,
            remap = false
        ),
        require = 1,
        expect = 1,
        remap = false
    )
    private static IIrisAccessor actinium$filterInactiveIrisAccessor(IIrisAccessor accessor) {
        return DistantHorizonsIrisAccessorState.activeAccessor(accessor);
    }
}
