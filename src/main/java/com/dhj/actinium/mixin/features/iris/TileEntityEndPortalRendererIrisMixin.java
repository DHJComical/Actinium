package com.dhj.actinium.mixin.features.iris;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.coderbot.iris.debug.IrisGlDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityEndPortalRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(TileEntityEndPortalRenderer.class)
public abstract class TileEntityEndPortalRendererIrisMixin {
    @Unique
    private static final ResourceLocation actinium$END_SKY_TEXTURE = new ResourceLocation("textures/environment/end_sky.png");
    @Unique
    private static final ResourceLocation actinium$END_PORTAL_TEXTURE = new ResourceLocation("textures/entity/end_portal.png");
    @Unique
    private static final Random actinium$RANDOM = new Random(31100L);
    @Unique
    private static final Vector4f actinium$CLIP_POS = new Vector4f();
    @Unique
    private static int actinium$portalLogCount;

    @Shadow
    protected abstract int getPasses(double distanceSq);

    @Shadow
    protected abstract float getOffset();

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntityEndPortal;DDDFIF)V", at = @At("HEAD"), cancellable = true)
    private void actinium$renderGtnhPortal(
            TileEntityEndPortal te,
            double x,
            double y,
            double z,
            float partialTicks,
            int destroyStage,
            float alpha,
            CallbackInfo ci
    ) {
        ci.cancel();

        if (actinium$portalLogCount++ < 8) {
            IrisGlDebug.logDebugInfo("end-portal-projective type={} pos=[{},{},{}]", te.getClass().getName(), x, y, z);
        }

        Matrix4f modelView = new Matrix4f(GLStateManager.getModelViewMatrix());
        Matrix4f projection = new Matrix4f(GLStateManager.getProjectionMatrix());
        double distanceSq = x * x + y * y + z * z;
        int passes = this.getPasses(distanceSq);
        float topOffset = this.getOffset();
        boolean changedFogColor = false;

        GLStateManager.disableLighting();
        GLStateManager.enableTexture();
        actinium$RANDOM.setSeed(31100L);

        int activeTexture = GLStateManager.getActiveTextureUnit();
        GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
        GLStateManager.glMatrixMode(GL11.GL_TEXTURE);
        GLStateManager.glPushMatrix();
        GLStateManager.glLoadIdentity();
        GLStateManager.glMatrixMode(GL11.GL_MODELVIEW);

        try {
            for (int pass = 0; pass < passes; ++pass) {
                float colorScale = 2.0F / (18 - pass);

                if (pass == 0) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(actinium$END_SKY_TEXTURE);
                    colorScale = 0.15F;
                    GLStateManager.enableBlend();
                    GLStateManager.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                } else {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(actinium$END_PORTAL_TEXTURE);
                    changedFogColor = true;
                    Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
                }

                if (pass == 1) {
                    GLStateManager.enableBlend();
                    GLStateManager.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                }

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);

                float red = (actinium$RANDOM.nextFloat() * 0.5F + 0.1F) * colorScale;
                float green = (actinium$RANDOM.nextFloat() * 0.5F + 0.4F) * colorScale;
                float blue = (actinium$RANDOM.nextFloat() * 0.5F + 0.5F) * colorScale;

                if (pass == 0) {
                    red = 1.0F * colorScale;
                    green = 1.0F * colorScale;
                    blue = 1.0F * colorScale;
                }

                Matrix4f textureProjection = actinium$portalTextureProjection(pass, modelView, projection);

                actinium$drawFaces(buffer, te, x, y, z, topOffset, red, green, blue, textureProjection);

                tessellator.draw();
            }
        } finally {
            GLStateManager.glMatrixMode(GL11.GL_TEXTURE);
            GLStateManager.glPopMatrix();
            GLStateManager.glMatrixMode(GL11.GL_MODELVIEW);
            GLStateManager.glActiveTexture(GL13.GL_TEXTURE0 + activeTexture);
        }

        GLStateManager.disableBlend();
        GLStateManager.enableLighting();

        if (changedFogColor) {
            Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
        }
    }

    @Unique
    private static Matrix4f actinium$portalTextureProjection(int pass, Matrix4f modelView, Matrix4f projection) {
        float layer = pass + 1.0F;
        float time = (float) (Minecraft.getSystemTime() % 800000L) / 800000.0F;
        float scroll = (2.0F + layer / 1.5F) * time;
        float rotation = (float) Math.toRadians((layer * layer * 4321.0F + layer * 9.0F) * 2.0F);
        float scale = 4.5F - layer / 4.0F;

        return new Matrix4f()
                .translate(0.5F, 0.5F, 0.0F)
                .scale(0.5F, 0.5F, 1.0F)
                .translate(17.0F / layer, scroll, 0.0F)
                .rotateZ(rotation)
                .scale(scale, scale, 1.0F)
                .mul(projection)
                .mul(modelView);
    }

    @Unique
    private static void actinium$drawFaces(
            BufferBuilder buffer,
            TileEntityEndPortal te,
            double x,
            double y,
            double z,
            float topOffset,
            float red,
            float green,
            float blue,
            Matrix4f textureProjection
    ) {
        if (te.shouldRenderFace(EnumFacing.SOUTH)) {
            actinium$vertex(buffer, x, y, z + 1.0D, red, green, blue, textureProjection);
            actinium$vertex(buffer, x + 1.0D, y, z + 1.0D, red, green, blue, textureProjection);
            actinium$vertex(buffer, x + 1.0D, y + 1.0D, z + 1.0D, red, green, blue, textureProjection);
            actinium$vertex(buffer, x, y + 1.0D, z + 1.0D, red, green, blue, textureProjection);
        }

        if (te.shouldRenderFace(EnumFacing.NORTH)) {
            actinium$vertex(buffer, x, y + 1.0D, z, red, green, blue, textureProjection);
            actinium$vertex(buffer, x + 1.0D, y + 1.0D, z, red, green, blue, textureProjection);
            actinium$vertex(buffer, x + 1.0D, y, z, red, green, blue, textureProjection);
            actinium$vertex(buffer, x, y, z, red, green, blue, textureProjection);
        }

        if (te.shouldRenderFace(EnumFacing.EAST)) {
            actinium$vertex(buffer, x + 1.0D, y + 1.0D, z, red, green, blue, textureProjection);
            actinium$vertex(buffer, x + 1.0D, y + 1.0D, z + 1.0D, red, green, blue, textureProjection);
            actinium$vertex(buffer, x + 1.0D, y, z + 1.0D, red, green, blue, textureProjection);
            actinium$vertex(buffer, x + 1.0D, y, z, red, green, blue, textureProjection);
        }

        if (te.shouldRenderFace(EnumFacing.WEST)) {
            actinium$vertex(buffer, x, y, z, red, green, blue, textureProjection);
            actinium$vertex(buffer, x, y, z + 1.0D, red, green, blue, textureProjection);
            actinium$vertex(buffer, x, y + 1.0D, z + 1.0D, red, green, blue, textureProjection);
            actinium$vertex(buffer, x, y + 1.0D, z, red, green, blue, textureProjection);
        }

        if (te.shouldRenderFace(EnumFacing.DOWN)) {
            actinium$vertex(buffer, x, y, z, red, green, blue, textureProjection);
            actinium$vertex(buffer, x + 1.0D, y, z, red, green, blue, textureProjection);
            actinium$vertex(buffer, x + 1.0D, y, z + 1.0D, red, green, blue, textureProjection);
            actinium$vertex(buffer, x, y, z + 1.0D, red, green, blue, textureProjection);
        }

        if (te.shouldRenderFace(EnumFacing.UP)) {
            actinium$vertex(buffer, x, y + topOffset, z + 1.0D, red, green, blue, textureProjection);
            actinium$vertex(buffer, x + 1.0D, y + topOffset, z + 1.0D, red, green, blue, textureProjection);
            actinium$vertex(buffer, x + 1.0D, y + topOffset, z, red, green, blue, textureProjection);
            actinium$vertex(buffer, x, y + topOffset, z, red, green, blue, textureProjection);
        }
    }

    @Unique
    private static void actinium$vertex(
            BufferBuilder buffer,
            double x,
            double y,
            double z,
            float red,
            float green,
            float blue,
            Matrix4f textureProjection
    ) {
        actinium$CLIP_POS.set((float) x, (float) y, (float) z, 1.0F);
        textureProjection.transform(actinium$CLIP_POS);

        float invQ = Math.abs(actinium$CLIP_POS.w) > 1.0E-4F ? 1.0F / actinium$CLIP_POS.w : 1.0F;
        double u = actinium$CLIP_POS.x * invQ;
        double v = actinium$CLIP_POS.y * invQ;

        buffer.pos(x, y, z).tex(u, v).lightmap(240, 240).color(red, green, blue, 1.0F).endVertex();
    }
}
