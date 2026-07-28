package com.dhj.actinium.compat.ichunutil;

import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalViewportFactoryTest {
    @Test
    void convertsRelativeViewportBoxBackToWorldCoordinates() {
        AtomicReference<double[]> testedBox = new AtomicReference<>();
        Viewport viewport = PortalViewportFactory.create(128.0, 64.0, -32.0, (minX, minY, minZ, maxX, maxY, maxZ) -> {
            testedBox.set(new double[] { minX, minY, minZ, maxX, maxY, maxZ });
            return true;
        });

        assertTrue(viewport.isBoxVisible(130.0, 65.0, -29.0, 134.0, 70.0, -24.0));
        assertArrayEquals(
            new double[] { 130.0, 65.0, -29.0, 134.0, 70.0, -24.0 },
            testedBox.get()
        );
    }

    @Test
    void preservesPortalFrustumRejection() {
        Viewport viewport = PortalViewportFactory.create(12.0, 34.0, 56.0, (minX, minY, minZ, maxX, maxY, maxZ) -> false);

        assertFalse(viewport.isBoxVisible(11.0, 33.0, 55.0, 13.0, 35.0, 57.0));
    }
}
