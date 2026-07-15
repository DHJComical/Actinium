package com.dhj.actinium.render;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndPortalProjectionTest {
    private static final double EPSILON = 1.0E-5D;

    @Test
    void homogeneousCoordinatesMatchTheCompleteVanillaTextureMatrix() {
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(70.0D), 16.0F / 9.0F, 0.05F, 256.0F);
        Matrix4f modelView = new Matrix4f().rotateX(0.35F).rotateY(-0.2F).translate(-2.0F, -1.0F, -6.0F);
        EndPortalProjection portalProjection = new EndPortalProjection(projection, modelView);
        EndPortalLayers.Layer layer = EndPortalLayers.create(1, 0.25F).getFirst();

        EndPortalProjection.ProjectiveTexCoord actual = layer.projective(portalProjection.clip(1.25D, 0.75D, -0.5D));

        float vanillaLayer = 1.0F;
        float scroll = (2.0F + vanillaLayer / 1.5F) * 0.25F;
        float angle = (vanillaLayer * vanillaLayer * 4321.0F + vanillaLayer * 9.0F) * 2.0F;
        float scale = 4.5F - vanillaLayer / 4.0F;
        Matrix4f textureMatrix = new Matrix4f()
            .translate(0.5F, 0.5F, 0.0F)
            .scale(0.5F, 0.5F, 1.0F)
            .translate(17.0F / vanillaLayer, scroll, 0.0F)
            .rotateZ((float) Math.toRadians(angle))
            .scale(scale, scale, 1.0F)
            .mul(projection)
            .mul(modelView);
        Vector4f expected = textureMatrix.transform(new Vector4f(1.25F, 0.75F, -0.5F, 1.0F));

        assertEquals(expected.x, actual.s(), EPSILON);
        assertEquals(expected.y, actual.t(), EPSILON);
        assertEquals(expected.z, actual.r(), EPSILON);
        assertEquals(expected.w, actual.q(), EPSILON);
    }

    @Test
    void equalNdcCoordinatesWithDifferentQDivideToTheSameUv() {
        EndPortalLayers.Layer layer = EndPortalLayers.create(1, 0.25F).getFirst();
        EndPortalProjection.ProjectiveTexCoord near = layer.projective(
            new EndPortalProjection.ClipPosition(0.25D, -0.5D, 0.0D, 1.0D)
        );
        EndPortalProjection.ProjectiveTexCoord far = layer.projective(
            new EndPortalProjection.ClipPosition(1.0D, -2.0D, 0.0D, 4.0D)
        );

        assertEquals(near.dividedU(), far.dividedU(), EPSILON);
        assertEquals(near.dividedV(), far.dividedV(), EPSILON);
    }

    @Test
    void projectedUvSpanShrinksWithDistanceAndChangesWithViewAngle() {
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(70.0D), 16.0F / 9.0F, 0.05F, 256.0F);
        EndPortalLayers.Layer layer = EndPortalLayers.create(1, 0.25F).getFirst();
        EndPortalProjection forward = new EndPortalProjection(projection, new Matrix4f());

        double nearSpan = uSpan(forward, layer, -4.0D);
        double farSpan = uSpan(forward, layer, -16.0D);
        assertTrue(farSpan < nearSpan);

        EndPortalProjection angled = new EndPortalProjection(projection, new Matrix4f().rotateY(0.45F));
        assertNotEquals(nearSpan, uSpan(angled, layer, -4.0D));
    }

    private static double uSpan(EndPortalProjection projection, EndPortalLayers.Layer layer, double z) {
        double first = layer.projective(projection.clip(0.0D, 0.0D, z)).dividedU();
        double second = layer.projective(projection.clip(1.0D, 0.0D, z)).dividedU();
        return Math.abs(second - first);
    }
}
