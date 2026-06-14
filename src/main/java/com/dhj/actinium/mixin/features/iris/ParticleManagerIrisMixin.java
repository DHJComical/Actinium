package com.dhj.actinium.mixin.features.iris;

import com.gtnewhorizons.angelica.client.rendering.DeferredDrawBatcher;
import net.coderbot.iris.Iris;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;

@Mixin(ParticleManager.class)
public class ParticleManagerIrisMixin {
    @Unique
    private Viewport actinium$cullingViewport;

    @Unique
    private WorldRenderingPhase actinium$previousParticlePhase;

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void actinium$beginParticles(Entity entityIn, float partialTicks, CallbackInfo ci) {
        this.actinium$setupCullingViewport();
        this.actinium$beginParticlePhase();
    }

    @Inject(method = "renderParticles", at = @At("RETURN"))
    private void actinium$endParticles(Entity entityIn, float partialTicks, CallbackInfo ci) {
        DeferredDrawBatcher.exitAndFlush();
        this.actinium$endParticlePhase();
    }

    @Inject(method = "renderLitParticles", at = @At("HEAD"))
    private void actinium$beginLitParticles(Entity entityIn, float partialTicks, CallbackInfo ci) {
        this.actinium$setupCullingViewport();
        this.actinium$beginParticlePhase();
    }

    @Inject(method = "renderLitParticles", at = @At("RETURN"))
    private void actinium$endLitParticles(Entity entityIn, float partialTicks, CallbackInfo ci) {
        this.actinium$endParticlePhase();
    }

    @Inject(
        method = "renderParticles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/BufferBuilder;begin(ILnet/minecraft/client/renderer/vertex/VertexFormat;)V",
            shift = At.Shift.AFTER
        )
    )
    private void actinium$enterDeferredParticleBatch(Entity entityIn, float partialTicks, CallbackInfo ci) {
        DeferredDrawBatcher.enter();
    }

    @Inject(
        method = "renderParticles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;draw()V"
        )
    )
    private void actinium$exitDeferredParticleBatch(Entity entityIn, float partialTicks, CallbackInfo ci) {
        DeferredDrawBatcher.exitAndFlush();
    }

    /**
     * Matches GTNH Angelica's notfine particle fix: avoid the first depth-mask disable in the vanilla
     * particle loop and leave depth state under the render pipeline's control.
     */
    @Redirect(
        method = "renderParticles",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;depthMask(Z)V", ordinal = 0)
    )
    private void actinium$skipFirstParticleDepthMask(boolean flag) {
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
        CeleritasWorldRenderer renderer = CeleritasWorldRenderer.instanceNullable();
        this.actinium$cullingViewport = renderer != null ? renderer.getLastViewport() : null;
    }

    @Unique
    private void actinium$beginParticlePhase() {
        if (!Iris.enabled || !IrisApiV0Impl.INSTANCE.isShaderPackInUse()) {
            this.actinium$previousParticlePhase = null;
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline == null) {
            this.actinium$previousParticlePhase = null;
            return;
        }

        this.actinium$previousParticlePhase = pipeline.getPhase();
        pipeline.setPhase(WorldRenderingPhase.PARTICLES);
    }

    @Unique
    private void actinium$endParticlePhase() {
        if (this.actinium$previousParticlePhase == null) {
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            pipeline.setPhase(this.actinium$previousParticlePhase);
        }

        this.actinium$previousParticlePhase = null;
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
