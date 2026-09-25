package com.dhj.actinium.mixin.vintage.core.frustum;

import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.dhj.actinium.render.frustum.IClippingHelper;

@Mixin(ClippingHelperImpl.class)
public abstract class MixinClippingHelperImpl extends ClippingHelper implements IClippingHelper {
    @Unique
    private final FrustumIntersection celeritas$frustum = new FrustumIntersection();

    @Unique
    private final Matrix4f celeritas$vpMatrix = new Matrix4f();

    @Inject(method = "init", at = @At("RETURN"))
    private void updateJoml(CallbackInfo ci) {
        Matrix4f jomlProjection = new Matrix4f();
        jomlProjection.set(projectionMatrix);
        Matrix4f jomlModelview = new Matrix4f();
        jomlModelview.set(modelviewMatrix);

        // The raster occluder's matrix must carry rotation and projection but no camera
        // translation: the viewport transform already supplies the camera position, and
        // the occluder applies the camera-relative offset per section. Strip the
        // view-space translation (eye height, third-person offset, bobbing) here.
        Matrix4f rotationOnly = new Matrix4f(jomlModelview);
        rotationOnly.m30(0).m31(0).m32(0);
        this.celeritas$vpMatrix.set(jomlProjection).mul(rotationOnly);

        this.celeritas$frustum.set(jomlProjection.mul(jomlModelview), true);
    }

    @Override
    public FrustumIntersection celeritas$getJomlFrustum() {
        return this.celeritas$frustum;
    }

    @Override
    public Matrix4f celeritas$getVpMatrix() {
        return this.celeritas$vpMatrix;
    }
}

