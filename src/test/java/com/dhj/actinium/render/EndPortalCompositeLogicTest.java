package com.dhj.actinium.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndPortalCompositeLogicTest {
    @Test
    void precomposesOnlyNearShaderPackTiers() {
        assertFalse(EndPortalCompositeLogic.shouldPrecompose(false, 16));
        assertFalse(EndPortalCompositeLogic.shouldPrecompose(true, 13));
        assertTrue(EndPortalCompositeLogic.shouldPrecompose(true, 14));
        assertTrue(EndPortalCompositeLogic.shouldPrecompose(true, 15));
        assertTrue(EndPortalCompositeLogic.shouldPrecompose(true, 16));
        assertFalse(EndPortalCompositeLogic.shouldPrecompose(true, 17));
    }

    @Test
    void accumulatesOpaqueSkyAndEveryAdditivePortalLayer() {
        List<EndPortalLayers.Layer> layers = EndPortalLayers.create(3, 0.25F);

        EndPortalCompositeLogic.Color actual = EndPortalCompositeLogic.composite(
            layers,
            (texture, u, v) -> switch (texture) {
                case END_SKY -> new EndPortalCompositeLogic.Color(0.25F, 0.5F, 0.75F, 1.0F);
                case END_PORTAL -> new EndPortalCompositeLogic.Color(0.4F, 0.3F, 0.2F, 1.0F);
            },
            -0.3D,
            0.65D
        );

        EndPortalLayers.Layer sky = layers.getFirst();
        EndPortalLayers.Layer firstPortal = layers.get(1);
        EndPortalLayers.Layer secondPortal = layers.get(2);
        assertEquals(
            0.25F * sky.red() + 0.4F * (firstPortal.red() + secondPortal.red()),
            actual.red(),
            1.0E-6F
        );
        assertEquals(
            0.5F * sky.green() + 0.3F * (firstPortal.green() + secondPortal.green()),
            actual.green(),
            1.0E-6F
        );
        assertEquals(
            0.75F * sky.blue() + 0.2F * (firstPortal.blue() + secondPortal.blue()),
            actual.blue(),
            1.0E-6F
        );
        assertEquals(1.0F, actual.alpha());
    }

    @Test
    void cacheUpdatesAtMostOncePerFrame() {
        assertTrue(EndPortalCompositeLogic.needsUpdate(-1L, 40L));
        assertFalse(EndPortalCompositeLogic.needsUpdate(40L, 40L));
        assertTrue(EndPortalCompositeLogic.needsUpdate(40L, 41L));
    }

    @Test
    void passesTheLayerTransformCoordinatesToTheTextureSampler() {
        EndPortalLayers.Layer layer = EndPortalLayers.create(1, 0.25F).getFirst();
        double projectedX = -0.35D;
        double projectedY = 0.6D;
        double[] sampledCoordinate = new double[2];

        EndPortalCompositeLogic.composite(
            List.of(layer),
            (texture, u, v) -> {
                sampledCoordinate[0] = u;
                sampledCoordinate[1] = v;
                return new EndPortalCompositeLogic.Color(1.0F, 1.0F, 1.0F, 1.0F);
            },
            projectedX,
            projectedY
        );

        assertEquals(layer.u(projectedX, projectedY), sampledCoordinate[0], 1.0E-9D);
        assertEquals(layer.v(projectedX, projectedY), sampledCoordinate[1], 1.0E-9D);
    }
}
