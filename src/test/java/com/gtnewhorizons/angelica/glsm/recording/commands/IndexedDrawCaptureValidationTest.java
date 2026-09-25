package com.gtnewhorizons.angelica.glsm.recording.commands;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Validation paths of {@link IndexedDrawCapture} that reject the draw before any GL
 * interaction. Paths that warn via GLStateManager are not unit-testable without a GL
 * context (loading GLStateManager performs static GL queries).
 */
class IndexedDrawCaptureValidationTest {

    @Test
    void zeroIndexCountIsRejected() {
        assertNull(IndexedDrawCapture.create(GL11.GL_TRIANGLES, 0, GL11.GL_UNSIGNED_INT, 0, 7));
        assertNull(IndexedDrawCapture.createFromClientIndices(GL11.GL_TRIANGLES, 0, GL11.GL_UNSIGNED_INT, 0, 0));
    }

    @Test
    void nullClientIndexAddressIsRejected() {
        assertNull(IndexedDrawCapture.createFromClientIndices(GL11.GL_TRIANGLES, 4, GL11.GL_UNSIGNED_INT, 0L, 16));
    }
}
