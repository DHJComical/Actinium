package com.dhj.actinium.render;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.states.BlendState;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.List;

/**
 * Draws the Core Profile replacement for the legacy projective end portal renderer.
 */
public final class EndPortalRenderer {
    private static final Logger LOGGER = LogManager.getLogger("EndPortalRenderer");
    private static final ResourceLocation END_SKY_TEXTURE = new ResourceLocation("textures/environment/end_sky.png");
    private static final ResourceLocation END_PORTAL_TEXTURE = new ResourceLocation("textures/entity/end_portal.png");
    private static final VertexFormatElement PROJECTIVE_UV_4F = new VertexFormatElement(
        0,
        VertexFormatElement.EnumType.FLOAT,
        VertexFormatElement.EnumUsage.UV,
        4
    );
    private static final VertexFormat PORTAL_VERTEX_FORMAT = createVertexFormat(DefaultVertexFormats.TEX_2F);
    private static final VertexFormat PROJECTIVE_PORTAL_VERTEX_FORMAT = createVertexFormat(PROJECTIVE_UV_4F);

    private EndPortalRenderer() {
    }

    /**
     * Renders one portal immediately so block entity shader state remains active for the draw call.
     *
     * @param portal portal or gateway supplying visible faces
     * @param x render-relative minimum X coordinate
     * @param y render-relative minimum Y coordinate
     * @param z render-relative minimum Z coordinate
     * @param topOffset renderer-specific height of the top face
     * @param vanillaLayerCount distance-dependent layer count returned by the vanilla renderer
     */
    public static void render(
        TileEntityEndPortal portal,
        double x,
        double y,
        double z,
        float topOffset,
        int vanillaLayerCount
    ) {
        float vanillaAnimationTime = (float) (Minecraft.getSystemTime() % 800000L) / 800000.0F;
        List<EndPortalLayers.Layer> layers = EndPortalLayers.create(vanillaLayerCount, vanillaAnimationTime);
        boolean shaderPackInUse = IrisApiV0Impl.INSTANCE.isShaderPackInUse();
        EndPortalProjection projection;
        List<EndPortalMesh.FaceQuad> faces;
        List<EndPortalMesh.Triangle> shaderTriangles;
        try {
            Matrix4f activeProjection = new Matrix4f(GLStateManager.getProjectionMatrix());
            Matrix4f activeModelView = new Matrix4f(GLStateManager.getModelViewMatrix());
            projection = new EndPortalProjection(activeProjection, activeModelView);
            faces = EndPortalMesh.visibleFaces(portal::shouldRenderFace, topOffset);
            shaderTriangles = shaderPackInUse
                ? EndPortalMesh.shaderTriangles(faces, projection, x, y, z, layers)
                : List.of();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid active matrix while building end portal geometry at {}", portal.getPos(), exception);
            return;
        }

        int activeTexture = GLStateManager.getActiveTextureUnit();
        GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
        int previousMatrixMode = GLStateManager.glGetInteger(GL11.GL_MATRIX_MODE);
        GLStateManager.glMatrixMode(GL11.GL_TEXTURE);
        GLStateManager.glPushMatrix();
        GLStateManager.glLoadIdentity();
        GLStateManager.glMatrixMode(previousMatrixMode);
        int previousTexture = GLStateManager.getBoundTextureForServerState();
        boolean textureEnabled = GLStateManager.glIsEnabled(GL11.GL_TEXTURE_2D);
        BlendState previousBlend = GLStateManager.getBlendState().copy();
        boolean blendEnabled = GLStateManager.getBlendMode().isEnabled();
        boolean lightingEnabled = GLStateManager.glIsEnabled(GL11.GL_LIGHTING);

        try {
            GLStateManager.enableTexture();
            GLStateManager.disableLighting();

            boolean precomposed = false;
            if (EndPortalCompositeLogic.shouldPrecompose(shaderPackInUse, layers.size())) {
                int compositeTexture = EndPortalCompositeRenderer.texture(layers);
                if (compositeTexture != 0) {
                    GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, compositeTexture);
                    GLStateManager.disableBlend();
                    EndPortalBlockEntityIdScope.run(() -> drawComposite(x, y, z, shaderTriangles));
                    precomposed = true;
                }
            }

            if (!precomposed) {
                EndPortalLayers.Layer skyLayer = layers.getFirst();
                bindTexture(skyLayer.texture());
                applyBlend(skyLayer.blend());
                drawLayers(x, y, z, layers, 0, 1, shaderPackInUse, faces, shaderTriangles, projection);

                if (layers.size() > 1) {
                    EndPortalLayers.Layer portalLayer = layers.get(1);
                    bindTexture(portalLayer.texture());
                    applyBlend(portalLayer.blend());
                    drawLayers(x, y, z, layers, 1, layers.size(), shaderPackInUse, faces, shaderTriangles, projection);
                }
            }
        } finally {
            GLStateManager.glBlendFuncSeparate(
                previousBlend.getSrcRgb(),
                previousBlend.getDstRgb(),
                previousBlend.getSrcAlpha(),
                previousBlend.getDstAlpha()
            );
            if (blendEnabled) {
                GLStateManager.enableBlend();
            } else {
                GLStateManager.disableBlend();
            }
            if (lightingEnabled) {
                GLStateManager.enableLighting();
            } else {
                GLStateManager.disableLighting();
            }
            GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            if (!textureEnabled) {
                GLStateManager.disableTexture();
            }
            GLStateManager.glMatrixMode(GL11.GL_TEXTURE);
            GLStateManager.glPopMatrix();
            GLStateManager.glMatrixMode(previousMatrixMode);
            GLStateManager.glActiveTexture(GL13.GL_TEXTURE0 + activeTexture);
        }
    }

    private static void bindTexture(EndPortalLayers.Texture texture) {
        ResourceLocation location = switch (texture) {
            case END_SKY -> END_SKY_TEXTURE;
            case END_PORTAL -> END_PORTAL_TEXTURE;
        };
        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
    }

    private static void applyBlend(EndPortalLayers.Blend blend) {
        switch (blend) {
            case ALPHA -> {
                GLStateManager.enableBlend();
                GLStateManager.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            case ADDITIVE -> {
                GLStateManager.enableBlend();
                GLStateManager.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            }
        }
    }

    private static void drawLayers(
        double x,
        double y,
        double z,
        List<EndPortalLayers.Layer> layers,
        int fromIndex,
        int toIndex,
        boolean shaderPackInUse,
        List<EndPortalMesh.FaceQuad> faces,
        List<EndPortalMesh.Triangle> shaderTriangles,
        EndPortalProjection projection
    ) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(
            shaderPackInUse ? GL11.GL_TRIANGLES : GL11.GL_QUADS,
            shaderPackInUse ? PORTAL_VERTEX_FORMAT : PROJECTIVE_PORTAL_VERTEX_FORMAT
        );
        for (int index = fromIndex; index < toIndex; index++) {
            EndPortalLayers.Layer layer = layers.get(index);
            if (shaderPackInUse) {
                EndPortalGeometry.emitDivided(
                    shaderTriangles,
                    layer,
                    x,
                    y,
                    z,
                    (face, vertexX, vertexY, vertexZ, u, v, red, green, blue, alpha, lightU, lightV, normalX, normalY, normalZ) ->
                        buffer.pos(vertexX, vertexY, vertexZ)
                            .color(red, green, blue, alpha)
                            .tex(u, v)
                            .lightmap(lightU, lightV)
                            .normal(normalX, normalY, normalZ)
                            .endVertex()
                );
            } else {
                EndPortalGeometry.emitHomogeneous(
                    faces,
                    projection,
                    x,
                    y,
                    z,
                    layer,
                    (face, vertexX, vertexY, vertexZ, s, t, r, q, red, green, blue, alpha, lightU, lightV, normalX, normalY, normalZ) -> {
                        buffer.pos(vertexX, vertexY, vertexZ).color(red, green, blue, alpha);
                        ((ProjectiveTexCoordBuffer) buffer).actinium$projectiveTexCoord(s, t, r, q);
                        buffer.lightmap(lightU, lightV).normal(normalX, normalY, normalZ).endVertex();
                    }
                );
            }
        }
        buffer.finishDrawing();
        if (buffer.getVertexCount() > 0) {
            VanillaBufferBuilderRenderer.draw(buffer, "EndPortal");
        } else {
            buffer.reset();
        }
    }

    private static void drawComposite(
        double x,
        double y,
        double z,
        List<EndPortalMesh.Triangle> shaderTriangles
    ) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, PORTAL_VERTEX_FORMAT);
        EndPortalGeometry.emitComposite(
            shaderTriangles,
            x,
            y,
            z,
            (face, vertexX, vertexY, vertexZ, u, v, red, green, blue, alpha, lightU, lightV, normalX, normalY, normalZ) ->
                buffer.pos(vertexX, vertexY, vertexZ)
                    .color(red, green, blue, alpha)
                    .tex(u, v)
                    .lightmap(lightU, lightV)
                    .normal(normalX, normalY, normalZ)
                    .endVertex()
        );
        buffer.finishDrawing();
        if (buffer.getVertexCount() > 0) {
            VanillaBufferBuilderRenderer.draw(buffer, "EndPortalComposite");
        } else {
            buffer.reset();
        }
    }

    private static VertexFormat createVertexFormat(VertexFormatElement primaryUv) {
        return new VertexFormat()
            .addElement(DefaultVertexFormats.POSITION_3F)
            .addElement(DefaultVertexFormats.COLOR_4UB)
            .addElement(primaryUv)
            .addElement(DefaultVertexFormats.TEX_2S)
            .addElement(DefaultVertexFormats.NORMAL_3B)
            .addElement(DefaultVertexFormats.PADDING_1B);
    }
}
