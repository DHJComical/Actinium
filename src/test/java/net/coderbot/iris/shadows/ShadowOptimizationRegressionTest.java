package net.coderbot.iris.shadows;

import com.gtnewhorizons.angelica.compat.toremove.MatrixStack;
import net.coderbot.iris.shadow.ShadowMatrices;
import net.coderbot.iris.shadows.frustum.BoxCuller;
import net.coderbot.iris.shadows.frustum.advanced.AdvancedShadowCullingFrustum;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowOptimizationRegressionTest {
    private static final float GRID_SIZE = 4.0f;
    private static final float EPSILON = 0.0001f;

    @Test
    void gridSnappingKeepsAWorldPointStableInsideOnePositiveGridCell() {
        Vector3f nearCellStart = projectWorldPoint(20.0f, 4.1);
        Vector3f nearCellEnd = projectWorldPoint(20.0f, 7.9);

        assertVectorEquals(nearCellStart, nearCellEnd);
        assertEquals(14.0f, nearCellStart.x(), EPSILON);
    }

    @Test
    void gridSnappingMovesAWorldPointByOneGridSizeWhenCrossingPositiveGridCell() {
        Vector3f beforeBoundary = projectWorldPoint(20.0f, 7.9);
        Vector3f afterBoundary = projectWorldPoint(20.0f, 8.1);

        assertEquals(GRID_SIZE, beforeBoundary.x() - afterBoundary.x(), EPSILON);
    }

    @Test
    void gridSnappingUsesTheSameStableOriginForNegativeCoordinates() {
        Vector3f nearCellStart = projectWorldPoint(20.0f, -4.1);
        Vector3f nearCellEnd = projectWorldPoint(20.0f, -7.9);
        Vector3f acrossBoundary = projectWorldPoint(20.0f, -8.1);

        assertVectorEquals(nearCellStart, nearCellEnd);
        assertEquals(GRID_SIZE, acrossBoundary.x() - nearCellEnd.x(), EPSILON);
    }

    @Test
    void boxCullerAcceptsIntersectingBoundsAndRejectsBoundsOutsideTheDistanceBox() {
        BoxCuller culler = new BoxCuller(8.0);
        culler.setPosition(10.0, 20.0, 30.0);

        assertFalse(culler.isCulled(17.5f, 19.0f, 29.0f, 18.0f, 21.0f, 31.0f));
        assertTrue(culler.isCulled(18.01f, 19.0f, 29.0f, 19.0f, 21.0f, 31.0f));
        assertFalse(culler.isCulledViewRelative(-8.0f, -1.0f, -1.0f, -7.0f, 1.0f, 1.0f));
        assertTrue(culler.isCulledViewRelative(-9.0f, -1.0f, -1.0f, -8.01f, 1.0f, 1.0f));
    }

    @Test
    void advancedFrustumKeepsVisibleBoundsAndRejectsDistanceAndFrustumOutliers() {
        BoxCuller distanceCuller = new BoxCuller(16.0);
        AdvancedShadowCullingFrustum frustum = new AdvancedShadowCullingFrustum();
        frustum.init(new Matrix4f(), new Matrix4f(), new Vector3f(0.0f, 0.0f, 1.0f), distanceCuller);
        frustum.setPosition(0.0, 0.0, 0.0);

        assertTrue(frustum.testAab(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f));
        assertFalse(frustum.testAab(17.0f, -0.5f, -0.5f, 18.0f, 0.5f, 0.5f));
        assertFalse(frustum.testAab(1.1f, -0.5f, -0.5f, 1.5f, 0.5f, 0.5f));
    }

    private static Vector3f projectWorldPoint(float worldX, double cameraX) {
        MatrixStack matrixStack = new MatrixStack();
        ShadowMatrices.snapModelViewToGrid(matrixStack, GRID_SIZE, cameraX, 0.0, 0.0);
        return matrixStack.peek().getModel().transformPosition(new Vector3f(worldX - (float) cameraX, 0.0f, 0.0f));
    }

    private static void assertVectorEquals(Vector3f expected, Vector3f actual) {
        assertEquals(expected.x(), actual.x(), EPSILON);
        assertEquals(expected.y(), actual.y(), EPSILON);
        assertEquals(expected.z(), actual.z(), EPSILON);
    }
}
