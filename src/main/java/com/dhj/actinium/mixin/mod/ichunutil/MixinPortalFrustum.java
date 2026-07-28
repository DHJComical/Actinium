package com.dhj.actinium.mixin.mod.ichunutil;

import com.dhj.actinium.compat.ichunutil.PortalViewportFactory;
import com.dhj.actinium.compat.ichunutil.PortalViewportProvider;
import me.ichun.mods.ichunutil.common.module.worldportals.client.render.culling.Frustum;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Frustum.class, remap = false)
public abstract class MixinPortalFrustum implements PortalViewportProvider {
    @Shadow
    private double xPosition;

    @Shadow
    private double yPosition;

    @Shadow
    private double zPosition;

    @Shadow
    public abstract boolean isBoxInFrustum(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
    );

    @Override
    public Viewport sodium$createViewport() {
        return PortalViewportFactory.create(
            this.xPosition,
            this.yPosition,
            this.zPosition,
            this::isBoxInFrustum
        );
    }
}
