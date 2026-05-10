package com.dhj.actinium.mixin.features.iris;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.renderer.entity.RenderDragon;
import net.minecraft.entity.boss.EntityDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderDragon.class)
public class RenderDragonIrisMixin {
    @Unique
    private static final NamespacedId ACTINIUM$END_CRYSTAL_BEAM = new NamespacedId("minecraft", "end_crystal_beam");

    @Unique
    private int actinium$previousEntityId = Integer.MIN_VALUE;

    @Inject(
        method = "doRender(Lnet/minecraft/entity/boss/EntityDragon;DDDFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderDragon;renderCrystalBeams(DDDFDDDIDDD)V", shift = At.Shift.BEFORE)
    )
    private void actinium$setBeamEntityId(
        EntityDragon dragon,
        double x,
        double y,
        double z,
        float entityYaw,
        float partialTicks,
        CallbackInfo ci
    ) {
        Object2IntFunction<NamespacedId> entityIdMap = BlockRenderingSettings.INSTANCE.getEntityIds();
        if (entityIdMap == null) {
            return;
        }

        this.actinium$previousEntityId = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        CapturedRenderingState.INSTANCE.setCurrentEntity(Math.max(0, entityIdMap.getInt(ACTINIUM$END_CRYSTAL_BEAM)));
    }

    @Inject(
        method = "doRender(Lnet/minecraft/entity/boss/EntityDragon;DDDFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderDragon;renderCrystalBeams(DDDFDDDIDDD)V", shift = At.Shift.AFTER)
    )
    private void actinium$restoreBeamEntityId(
        EntityDragon dragon,
        double x,
        double y,
        double z,
        float entityYaw,
        float partialTicks,
        CallbackInfo ci
    ) {
        if (this.actinium$previousEntityId != Integer.MIN_VALUE) {
            CapturedRenderingState.INSTANCE.setCurrentEntity(this.actinium$previousEntityId);
            this.actinium$previousEntityId = Integer.MIN_VALUE;
        }
    }
}
