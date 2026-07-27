package com.dhj.actinium.mixin.mod.dh;

import com.dhj.actinium.compat.dh.DistantHorizonsIrisAccessorState;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.seibel.distanthorizons.core.render.renderer.LodRenderer;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Restores the configured far fade whenever the registered Iris accessor has no active shader pack. */
@Mixin(value = LodRenderer.class, remap = false)
public abstract class MixinLodRenderer {
    @ModifyExpressionValue(
        method = "renderTerrain(Lcom/seibel/distanthorizons/core/render/RenderParams;"
            + "Lcom/seibel/distanthorizons/core/wrapperInterfaces/minecraft/IProfilerWrapper;Z)V",
        at = @At(
            value = "FIELD",
            target = "Lcom/seibel/distanthorizons/core/render/renderer/LodRenderer;IRIS_ACCESSOR:"
                + "Lcom/seibel/distanthorizons/core/wrapperInterfaces/modAccessor/IIrisAccessor;",
            opcode = Opcodes.GETSTATIC,
            remap = false
        ),
        require = 1,
        expect = 1,
        remap = false
    )
    private IIrisAccessor actinium$filterInactiveIrisAccessor(IIrisAccessor accessor) {
        return DistantHorizonsIrisAccessorState.activeAccessor(accessor);
    }
}
