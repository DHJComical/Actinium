package com.dhj.actinium.render;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiGlStateBoundaryEntitySurfaceTest {
    @AfterEach
    void verifySurfaceIsBalanced() {
        // A leaked beginEntitySurface() would leave the predicate on for later tests.
        assertFalse(GuiGlStateBoundary.isEntityGuiSurface(), "a test leaked a beginEntitySurface()");
    }

    @Test
    void startsOutsideEntityGuiSurface() {
        assertFalse(GuiGlStateBoundary.isEntityGuiSurface());
    }

    @Test
    void surfaceIsActiveUntilRestored() {
        GuiGlStateBoundary.EntitySurfaceState surface = GuiGlStateBoundary.beginEntitySurface();
        try {
            assertTrue(GuiGlStateBoundary.isEntityGuiSurface());
        } finally {
            surface.restore();
        }
        assertFalse(GuiGlStateBoundary.isEntityGuiSurface());
    }

    @Test
    void nestedSurfaceStaysActiveUntilOutermostRestore() {
        GuiGlStateBoundary.EntitySurfaceState outer = GuiGlStateBoundary.beginEntitySurface();
        try {
            GuiGlStateBoundary.EntitySurfaceState inner = GuiGlStateBoundary.beginEntitySurface();
            try {
                assertTrue(GuiGlStateBoundary.isEntityGuiSurface());
            } finally {
                inner.restore();
            }
            assertTrue(GuiGlStateBoundary.isEntityGuiSurface());
        } finally {
            outer.restore();
        }
        assertFalse(GuiGlStateBoundary.isEntityGuiSurface());
    }

    @Test
    void restoringTheSameTokenTwiceFails() {
        GuiGlStateBoundary.EntitySurfaceState surface = GuiGlStateBoundary.beginEntitySurface();
        surface.restore();

        assertThrows(IllegalStateException.class, surface::restore);
        assertFalse(GuiGlStateBoundary.isEntityGuiSurface());
    }
}
