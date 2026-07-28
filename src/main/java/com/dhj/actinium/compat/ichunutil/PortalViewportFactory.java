package com.dhj.actinium.compat.ichunutil;

import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.joml.Vector3d;

import java.util.Objects;

/**
 * Adapts iChun's world-space portal frustum to Celeritas' camera-relative viewport contract.
 */
public final class PortalViewportFactory {
    private PortalViewportFactory() {
    }

    /**
     * Creates a viewport at the portal camera position while preserving iChun's world-space box test.
     */
    public static Viewport create(
        double cameraX,
        double cameraY,
        double cameraZ,
        WorldBoxVisibility visibility
    ) {
        Objects.requireNonNull(visibility, "visibility");
        return new Viewport(
            (minX, minY, minZ, maxX, maxY, maxZ) -> visibility.isBoxVisible(
                minX + cameraX,
                minY + cameraY,
                minZ + cameraZ,
                maxX + cameraX,
                maxY + cameraY,
                maxZ + cameraZ
            ),
            new Vector3d(cameraX, cameraY, cameraZ)
        );
    }
}
