package com.gtnewhorizons.angelica.glsm.states;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.ARBImaging;
import org.lwjgl.opengl.GL11;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Behavioral pins for {@link MatrixMode#getMatrix()} (issue #134): the mode-to-pname
 * mapping must cover {@code GL_COLOR} so matrix queries in color-matrix mode resolve to
 * {@code GL_COLOR_MATRIX}, and an out-of-set mode must fall back to
 * {@code GL_MODELVIEW_MATRIX} instead of throwing, so Minecraft's
 * {@code resetGlStates()} recovery path can always complete.
 *
 * <p>{@code setMode} is not exercised here: it calls
 * {@code GLStateManager.shouldBypassCache()}, whose class initializer needs a live GL
 * context, so the mode is assigned through the package-visible field instead.</p>
 */
class MatrixModeTest {

    @Test
    void colorModeMapsToArbColorMatrix() {
        MatrixMode matrixMode = new MatrixMode();
        matrixMode.mode = GL11.GL_COLOR;
        assertEquals(ARBImaging.GL_COLOR_MATRIX, matrixMode.getMatrix());
    }

    @Test
    void coreModesMapToTheirMatrixPnames() {
        MatrixMode matrixMode = new MatrixMode();
        matrixMode.mode = GL11.GL_MODELVIEW;
        assertEquals(GL11.GL_MODELVIEW_MATRIX, matrixMode.getMatrix());
        matrixMode.mode = GL11.GL_PROJECTION;
        assertEquals(GL11.GL_PROJECTION_MATRIX, matrixMode.getMatrix());
        matrixMode.mode = GL11.GL_TEXTURE;
        assertEquals(GL11.GL_TEXTURE_MATRIX, matrixMode.getMatrix());
    }

    @Test
    void unknownModeFallsBackToModelviewMatrix() {
        MatrixMode matrixMode = new MatrixMode();
        matrixMode.mode = GL11.GL_MODELVIEW - 1;
        assertEquals(GL11.GL_MODELVIEW_MATRIX, matrixMode.getMatrix());
    }
}
