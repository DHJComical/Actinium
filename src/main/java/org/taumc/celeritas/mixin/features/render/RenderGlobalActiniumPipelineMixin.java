package org.taumc.celeritas.mixin.features.render;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.shader.pipeline.ActiniumRenderPipeline;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.client.multiplayer.WorldClient;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.BufferBuilder;

@Mixin(RenderGlobal.class)
public class RenderGlobalActiniumPipelineMixin {
    @Shadow
    private WorldClient world;

    @Inject(method = "renderSky(FI)V", at = @At("HEAD"))
    private void actinium$beginSky(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.beginManagedSky(partialTicks);
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.head");
    }

    @Inject(method = "renderSky(FI)V", at = @At("RETURN"))
    private void actinium$endSky(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.endManagedSky();
    }

    @Inject(method = "renderClouds(FIDDD)V", at = @At("HEAD"))
    private void actinium$beginClouds(float partialTicks, int pass, double x, double y, double z, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.beginClouds();
        ActiniumRenderPipeline.INSTANCE.bindWorldStageProgram(partialTicks);
    }

    @Inject(method = "renderClouds(FIDDD)V", at = @At("RETURN"))
    private void actinium$endClouds(float partialTicks, int pass, double x, double y, double z, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.unbindWorldStageProgram();
        ActiniumRenderPipeline.INSTANCE.endClouds();
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V", ordinal = 0, shift = At.Shift.AFTER),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getRainStrength(F)F"))
    )
    private void actinium$preCelestialRotate(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.captureManagedSkyPreCelestialState(partialTicks);

        if (!ActiniumRenderPipeline.INSTANCE.shouldApplySunPathRotationToVanillaSky()) {
            return;
        }

        float sunPathRotation = ActiniumShaderPackManager.getActiveShaderProperties().getSunPathRotation();
        if (sunPathRotation != 0.0F) {
            GlStateManager.rotate(sunPathRotation, 0.0F, 0.0F, 1.0F);
        }

        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.preCelestialRotate");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V", ordinal = 1, shift = At.Shift.AFTER),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getRainStrength(F)F"))
    )
    private void actinium$postCelestialRotate(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.captureManagedSkyPostCelestialState(partialTicks);
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.postCelestialRotate");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFF)V", ordinal = 1, shift = At.Shift.AFTER)
    )
    private void actinium$preSkyList(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.renderShaderCoreSkyHorizon();
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.preSkyList");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V", ordinal = 0),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProvider;calcSunriseSunsetColors(FF)[F"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", ordinal = 0)
            )
    )
    private void actinium$clearSunriseFan(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.sunriseFan");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", ordinal = 0)
    )
    private void actinium$debugSkyTexturedEntry(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.skyTextured");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;disableTexture2D()V", ordinal = 0, shift = At.Shift.AFTER)
    )
    private void actinium$onSkyDisableTexture0(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.updateManagedSkyTextureState(false, partialTicks);
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.disableTexture0");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;disableTexture2D()V", ordinal = 1, shift = At.Shift.AFTER)
    )
    private void actinium$onSkyDisableTexture1(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.updateManagedSkyTextureState(false, partialTicks);
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.disableTexture1");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;disableTexture2D()V", ordinal = 2, shift = At.Shift.AFTER)
    )
    private void actinium$onSkyDisableTexture2(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.updateManagedSkyTextureState(false, partialTicks);
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.disableTexture2");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableTexture2D()V", ordinal = 0, shift = At.Shift.AFTER)
    )
    private void actinium$onSkyEnableTexture0(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.updateManagedSkyTextureState(true, partialTicks);
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.enableTexture0");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableTexture2D()V", ordinal = 1, shift = At.Shift.AFTER)
    )
    private void actinium$onSkyEnableTexture1(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.updateManagedSkyTextureState(true, partialTicks);
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.enableTexture1");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/vertex/VertexBuffer;drawArrays(I)V", ordinal = 2)
    )
    private void actinium$debugSkyHorizonVbo(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.horizonVbo");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V", ordinal = 2)
    )
    private void actinium$debugSkyHorizonCallList(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.horizonCallList");
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V", ordinal = 3)
    )
    private void actinium$debugSkyLowerCap(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.debugLogSkySegment("renderSky.lowerCap");
    }

    @WrapWithCondition(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/vertex/VertexBuffer;drawArrays(I)V", ordinal = 2))
    private boolean actinium$renderSky2Vbo(VertexBuffer vertexBuffer, int mode) {
        return !ActiniumRenderPipeline.INSTANCE.shouldSuppressVanillaSkyHorizonGeometry();
    }

    @WrapWithCondition(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V", ordinal = 2))
    private boolean actinium$renderSky2CallListBelowHorizon(int displayList) {
        return !ActiniumRenderPipeline.INSTANCE.shouldSuppressVanillaSkyHorizonGeometry();
    }

    @WrapWithCondition(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V", ordinal = 3))
    private boolean actinium$renderSky2CallList(int displayList) {
        return !ActiniumRenderPipeline.INSTANCE.shouldSuppressVanillaSkyHorizonGeometry();
    }

    @Inject(method = "drawSelectionBox", at = @At("HEAD"), cancellable = true)
    private void actinium$drawSelectionBox(EntityPlayer player, RayTraceResult hitResult, int execute, float partialTicks, CallbackInfo ci) {
        if (execute != 0 || hitResult.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }

        BlockPos blockPos = hitResult.getBlockPos();
        IBlockState blockState = this.world.getBlockState(blockPos);
        if (blockState.getMaterial() == Material.AIR || !this.world.getWorldBorder().contains(blockPos)) {
            ci.cancel();
            return;
        }

        AxisAlignedBB selectionBox = blockState.getSelectedBoundingBox(this.world, blockPos);
        if (selectionBox == null) {
            ci.cancel();
            return;
        }

        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        AxisAlignedBB shiftedBox = selectionBox.grow(0.002D).offset(-camX, -camY, -camZ);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.disableFog();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2.0F);
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_POINT);

        this.actinium$drawSelectionBoundingBox(shiftedBox, 0.0F, 0.0F, 0.0F, 0.4F);

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        ci.cancel();
    }

    @Unique
    private boolean actinium$shouldSuppressVanillaSkyGeometry() {
        return ActiniumShaderPackManager.areShadersEnabled()
                && ActiniumRenderPipeline.INSTANCE.shouldSuppressVanillaSkyGeometry();
    }

    @Unique
    private void actinium$drawSelectionBoundingBox(AxisAlignedBB box, float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        this.actinium$line(bufferBuilder, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, red, green, blue, alpha);
        this.actinium$line(bufferBuilder, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, red, green, blue, alpha);
        this.actinium$line(bufferBuilder, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, red, green, blue, alpha);
        this.actinium$line(bufferBuilder, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, red, green, blue, alpha);

        this.actinium$line(bufferBuilder, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, red, green, blue, alpha);
        this.actinium$line(bufferBuilder, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
        this.actinium$line(bufferBuilder, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, red, green, blue, alpha);
        this.actinium$line(bufferBuilder, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, red, green, blue, alpha);

        this.actinium$line(bufferBuilder, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, red, green, blue, alpha);
        this.actinium$line(bufferBuilder, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, red, green, blue, alpha);
        this.actinium$line(bufferBuilder, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
        this.actinium$line(bufferBuilder, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, red, green, blue, alpha);

        tessellator.draw();
    }

    @Unique
    private void actinium$line(BufferBuilder bufferBuilder,
                               double startX,
                               double startY,
                               double startZ,
                               double endX,
                               double endY,
                               double endZ,
                               float red,
                               float green,
                               float blue,
                               float alpha) {
        bufferBuilder.pos(startX, startY, startZ).color(red, green, blue, alpha).endVertex();
        bufferBuilder.pos(endX, endY, endZ).color(red, green, blue, alpha).endVertex();
    }
}
