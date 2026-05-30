package com.dhj.actinium.render;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class EndPortalBatchRenderer {
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("actinium.endPortalBatching", "true"));
    private static final ResourceLocation END_SKY_TEXTURE = new ResourceLocation("textures/environment/end_sky.png");
    private static final ResourceLocation END_PORTAL_TEXTURE = new ResourceLocation("textures/entity/end_portal.png");
    private static final Random RANDOM = new Random(31100L);
    private static final Vector4f CLIP_POS = new Vector4f();
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static boolean active;

    private EndPortalBatchRenderer() {
    }

    public static void begin() {
        if (!ENABLED) {
            return;
        }
        active = true;
        ENTRIES.clear();
    }

    public static void end() {
        if (!active) {
            return;
        }

        try {
            renderEntries(ENTRIES);
        } finally {
            ENTRIES.clear();
            active = false;
        }
    }

    public static boolean enqueue(TileEntityEndPortal te, double x, double y, double z, int passes, float topOffset) {
        if (!ENABLED || !active) {
            return false;
        }

        ENTRIES.add(new Entry(te, x, y, z, passes, topOffset));
        return true;
    }

    public static void renderImmediate(TileEntityEndPortal te, double x, double y, double z, int passes, float topOffset) {
        List<Entry> entries = new ArrayList<>(1);
        entries.add(new Entry(te, x, y, z, passes, topOffset));
        renderEntries(entries);
    }

    private static void renderEntries(List<Entry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        Matrix4f modelView = new Matrix4f(GLStateManager.getModelViewMatrix());
        Matrix4f projection = new Matrix4f(GLStateManager.getProjectionMatrix());
        int maxPasses = 0;
        for (Entry entry : entries) {
            maxPasses = Math.max(maxPasses, entry.passes);
        }

        boolean changedFogColor = false;

        GLStateManager.disableLighting();
        GLStateManager.enableTexture();
        RANDOM.setSeed(31100L);

        int activeTexture = GLStateManager.getActiveTextureUnit();
        GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
        GLStateManager.glMatrixMode(GL11.GL_TEXTURE);
        GLStateManager.glPushMatrix();
        GLStateManager.glLoadIdentity();
        GLStateManager.glMatrixMode(GL11.GL_MODELVIEW);

        try {
            for (int pass = 0; pass < maxPasses; ++pass) {
                float colorScale = 2.0F / (18 - pass);

                if (pass == 0) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(END_SKY_TEXTURE);
                    colorScale = 0.15F;
                    GLStateManager.enableBlend();
                    GLStateManager.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                } else {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(END_PORTAL_TEXTURE);
                    changedFogColor = true;
                    Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
                }

                if (pass == 1) {
                    GLStateManager.enableBlend();
                    GLStateManager.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                }

                float red = (RANDOM.nextFloat() * 0.5F + 0.1F) * colorScale;
                float green = (RANDOM.nextFloat() * 0.5F + 0.4F) * colorScale;
                float blue = (RANDOM.nextFloat() * 0.5F + 0.5F) * colorScale;

                if (pass == 0) {
                    red = colorScale;
                    green = colorScale;
                    blue = colorScale;
                }

                Matrix4f textureProjection = portalTextureProjection(pass, modelView, projection);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);

                boolean hasVertices = false;
                for (Entry entry : entries) {
                    if (pass < entry.passes) {
                        hasVertices |= drawFaces(buffer, entry, red, green, blue, textureProjection);
                    }
                }

                if (hasVertices) {
                    tessellator.draw();
                } else {
                    buffer.reset();
                }
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

    private static Matrix4f portalTextureProjection(int pass, Matrix4f modelView, Matrix4f projection) {
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

    private static boolean drawFaces(BufferBuilder buffer, Entry entry, float red, float green, float blue, Matrix4f textureProjection) {
        boolean hasVertices = false;
        TileEntityEndPortal te = entry.te;
        double x = entry.x;
        double y = entry.y;
        double z = entry.z;

        if (te.shouldRenderFace(EnumFacing.SOUTH)) {
            vertex(buffer, x, y, z + 1.0D, red, green, blue, textureProjection);
            vertex(buffer, x + 1.0D, y, z + 1.0D, red, green, blue, textureProjection);
            vertex(buffer, x + 1.0D, y + 1.0D, z + 1.0D, red, green, blue, textureProjection);
            vertex(buffer, x, y + 1.0D, z + 1.0D, red, green, blue, textureProjection);
            hasVertices = true;
        }

        if (te.shouldRenderFace(EnumFacing.NORTH)) {
            vertex(buffer, x, y + 1.0D, z, red, green, blue, textureProjection);
            vertex(buffer, x + 1.0D, y + 1.0D, z, red, green, blue, textureProjection);
            vertex(buffer, x + 1.0D, y, z, red, green, blue, textureProjection);
            vertex(buffer, x, y, z, red, green, blue, textureProjection);
            hasVertices = true;
        }

        if (te.shouldRenderFace(EnumFacing.EAST)) {
            vertex(buffer, x + 1.0D, y + 1.0D, z, red, green, blue, textureProjection);
            vertex(buffer, x + 1.0D, y + 1.0D, z + 1.0D, red, green, blue, textureProjection);
            vertex(buffer, x + 1.0D, y, z + 1.0D, red, green, blue, textureProjection);
            vertex(buffer, x + 1.0D, y, z, red, green, blue, textureProjection);
            hasVertices = true;
        }

        if (te.shouldRenderFace(EnumFacing.WEST)) {
            vertex(buffer, x, y, z, red, green, blue, textureProjection);
            vertex(buffer, x, y, z + 1.0D, red, green, blue, textureProjection);
            vertex(buffer, x, y + 1.0D, z + 1.0D, red, green, blue, textureProjection);
            vertex(buffer, x, y + 1.0D, z, red, green, blue, textureProjection);
            hasVertices = true;
        }

        if (te.shouldRenderFace(EnumFacing.DOWN)) {
            vertex(buffer, x, y, z, red, green, blue, textureProjection);
            vertex(buffer, x + 1.0D, y, z, red, green, blue, textureProjection);
            vertex(buffer, x + 1.0D, y, z + 1.0D, red, green, blue, textureProjection);
            vertex(buffer, x, y, z + 1.0D, red, green, blue, textureProjection);
            hasVertices = true;
        }

        if (te.shouldRenderFace(EnumFacing.UP)) {
            vertex(buffer, x, y + entry.topOffset, z + 1.0D, red, green, blue, textureProjection);
            vertex(buffer, x + 1.0D, y + entry.topOffset, z + 1.0D, red, green, blue, textureProjection);
            vertex(buffer, x + 1.0D, y + entry.topOffset, z, red, green, blue, textureProjection);
            vertex(buffer, x, y + entry.topOffset, z, red, green, blue, textureProjection);
            hasVertices = true;
        }

        return hasVertices;
    }

    private static void vertex(BufferBuilder buffer, double x, double y, double z, float red, float green, float blue, Matrix4f textureProjection) {
        CLIP_POS.set((float) x, (float) y, (float) z, 1.0F);
        textureProjection.transform(CLIP_POS);

        float invQ = Math.abs(CLIP_POS.w) > 1.0E-4F ? 1.0F / CLIP_POS.w : 1.0F;
        double u = CLIP_POS.x * invQ;
        double v = CLIP_POS.y * invQ;

        buffer.pos(x, y, z).tex(u, v).lightmap(240, 240).color(red, green, blue, 1.0F).endVertex();
    }

    private static final class Entry {
        private final TileEntityEndPortal te;
        private final double x;
        private final double y;
        private final double z;
        private final int passes;
        private final float topOffset;

        private Entry(TileEntityEndPortal te, double x, double y, double z, int passes, float topOffset) {
            this.te = te;
            this.x = x;
            this.y = y;
            this.z = z;
            this.passes = passes;
            this.topOffset = topOffset;
        }
    }
}
