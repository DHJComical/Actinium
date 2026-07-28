package com.dhj.actinium.mixin.mod.ichunutil;

import com.dhj.actinium.compat.ichunutil.PortalRenderState;
import com.dhj.actinium.mixin.vintage.core.terrain.AccessorActiveRenderInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.ichun.mods.ichunutil.common.module.worldportals.client.render.WorldPortalRenderer;
import me.ichun.mods.ichunutil.common.module.worldportals.common.portal.WorldPortal;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = WorldPortalRenderer.class, remap = false)
public class MixinWorldPortalRenderer {
    @WrapMethod(
        method = "renderWorldPortal(Lnet/minecraft/client/Minecraft;"
            + "Lme/ichun/mods/ichunutil/common/module/worldportals/common/portal/WorldPortal;"
            + "Lnet/minecraft/entity/Entity;[F[FF)V"
    )
    private static void actinium$preserveRenderState(
        Minecraft minecraft,
        WorldPortal portal,
        Entity entity,
        float[] position,
        float[] rotation,
        float partialTicks,
        Operation<Void> original
    ) {
        Vec3d cameraPosition = AccessorActiveRenderInfo.getPosition();
        float rotationX = AccessorActiveRenderInfo.getRotationX();
        float rotationXZ = AccessorActiveRenderInfo.getRotationXZ();
        float rotationZ = AccessorActiveRenderInfo.getRotationZ();
        float rotationYZ = AccessorActiveRenderInfo.getRotationYZ();
        float rotationXY = AccessorActiveRenderInfo.getRotationXY();

        try {
            PortalRenderState.preserve(
                AccessorActiveRenderInfo.getProjectionMatrix(),
                AccessorActiveRenderInfo.getModelViewMatrix(),
                AccessorActiveRenderInfo.getObjectCoords(),
                AccessorActiveRenderInfo.getViewportBuffer(),
                () -> original.call(minecraft, portal, entity, position, rotation, partialTicks)
            );
        } finally {
            AccessorActiveRenderInfo.setPosition(cameraPosition);
            AccessorActiveRenderInfo.setRotationX(rotationX);
            AccessorActiveRenderInfo.setRotationXZ(rotationXZ);
            AccessorActiveRenderInfo.setRotationZ(rotationZ);
            AccessorActiveRenderInfo.setRotationYZ(rotationYZ);
            AccessorActiveRenderInfo.setRotationXY(rotationXY);
        }
    }
}
