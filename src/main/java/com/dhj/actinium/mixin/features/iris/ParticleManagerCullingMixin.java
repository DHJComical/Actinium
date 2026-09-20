package com.dhj.actinium.mixin.features.iris;

import com.dhj.actinium.render.terrain.ActiniumWorldRenderer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import dhj.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips particles whose bounding box falls outside the last rendered viewport.
 *
 * <p>ParticleCulling declares its own plain {@code @Redirect} on exactly the same two
 * {@code Particle.renderParticle} call sites used here (one in {@code renderParticles},
 * one in {@code renderLitParticles}). Plain redirects cannot be chained: the injector that
 * runs second finds no INVOKE left to redirect and aborts startup with an
 * {@code InjectionError}. Because ParticleCulling already culls particles, this class is
 * skipped entirely by {@code IrisMixinConfigPlugin} when that mod is detected, so its
 * functionality is not lost while the conflicting redirects never coexist.
 */
@Mixin(ParticleManager.class)
public class ParticleManagerCullingMixin {
    @Unique
    private Viewport actinium$cullingViewport;

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void actinium$setupCullingViewportForParticles(Entity entityIn, float partialTicks, CallbackInfo ci) {
        this.actinium$setupCullingViewport();
    }

    @Inject(method = "renderLitParticles", at = @At("HEAD"))
    private void actinium$setupCullingViewportForLitParticles(Entity entityIn, float partialTicks, CallbackInfo ci) {
        this.actinium$setupCullingViewport();
    }

    @Redirect(
        method = "renderParticles",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V")
    )
    private void actinium$cullParticle(
        Particle particle,
        BufferBuilder buffer,
        Entity entityIn,
        float partialTicks,
        float rotationX,
        float rotationZ,
        float rotationYZ,
        float rotationXY,
        float rotationXZ
    ) {
        this.actinium$renderParticleIfVisible(particle, buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    }

    @Redirect(
        method = "renderLitParticles",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V")
    )
    private void actinium$cullLitParticle(
        Particle particle,
        BufferBuilder buffer,
        Entity entityIn,
        float partialTicks,
        float rotationX,
        float rotationZ,
        float rotationYZ,
        float rotationXY,
        float rotationXZ
    ) {
        this.actinium$renderParticleIfVisible(particle, buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    }

    @Unique
    private void actinium$setupCullingViewport() {
        ActiniumWorldRenderer renderer = ActiniumWorldRenderer.instanceNullable();
        this.actinium$cullingViewport = renderer != null ? renderer.getLastViewport() : null;
    }

    @Unique
    private void actinium$renderParticleIfVisible(
        Particle particle,
        BufferBuilder buffer,
        Entity entityIn,
        float partialTicks,
        float rotationX,
        float rotationZ,
        float rotationYZ,
        float rotationXY,
        float rotationXZ
    ) {
        AxisAlignedBB box = particle.getBoundingBox();
        if (this.actinium$cullingViewport == null
                || box == null
                || box == TileEntity.INFINITE_EXTENT_AABB
                || this.actinium$cullingViewport.isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)) {
            particle.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
        }
    }
}
