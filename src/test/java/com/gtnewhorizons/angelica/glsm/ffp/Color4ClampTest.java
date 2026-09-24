package com.gtnewhorizons.angelica.glsm.ffp;

import com.gtnewhorizons.angelica.glsm.states.Color4;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies the GL {@code glColor} API-boundary clamp used by
 * {@code GLStateManager.changeColor}. Per GL semantics floating-point color
 * components are clamped to [0,1] when specified, so the GLSM current-color
 * cache must never store a negative component.
 *
 * <p>Regression for GalaxySpace/AsmodeusCore sky rendering (issue #164):
 * {@code SkyProviderBase.render()} sets the night sky dome color with
 * {@code glColor3f(skyColor - playerY / 400)}, which goes negative at night
 * (skyColor near zero, playerY / 400 positive). Storing the raw negative let
 * {@link Uniforms#sanitizeUniformColor} mistake it for the
 * {@code clearCurrentColor} dirty sentinel and upload opaque white, turning
 * the night sky white.</p>
 *
 * <p>{@code GLStateManager} itself cannot be loaded without a GL context (its
 * static initializers query the backend), so the clamp is exercised through
 * the shared pure function it delegates to.</p>
 */
class Color4ClampTest {

    @Test
    void negativeComponentClampsToZero() {
        // GalaxySpace night sky: skyColor (≈0) - posY/400 (0.16 at y=64).
        assertEquals(0.0F, Color4.clamp01(-0.16F));
        assertEquals(0.0F, Color4.clamp01(-1.0F));
    }

    @Test
    void componentAboveOneClampsToOne() {
        assertEquals(1.0F, Color4.clamp01(1.5F));
        assertEquals(1.0F, Color4.clamp01(1.0F));
    }

    @Test
    void inRangeComponentPassesThroughUnchanged() {
        assertEquals(0.0F, Color4.clamp01(0.0F));
        assertEquals(0.5F, Color4.clamp01(0.5F));
    }

    @Test
    void clampedNightSkyColorSurvivesUniformSanitization() {
        // End-to-end issue scenario: the night dome color after the API clamp
        // must reach the FFP uniform as black, not be turned white by the
        // dirty-sentinel normalization.
        Color4 clamped = new Color4(
            Color4.clamp01(-0.16F),
            Color4.clamp01(-0.16F),
            Color4.clamp01(-0.13F),
            Color4.clamp01(1.0F));

        Color4 uploaded = Uniforms.sanitizeUniformColor(clamped);

        assertSame(clamped, uploaded, "a clamped color must not be mistaken for the dirty sentinel");
        assertEquals(0.0F, uploaded.getRed());
        assertEquals(0.0F, uploaded.getGreen());
        assertEquals(0.0F, uploaded.getBlue());
        assertEquals(1.0F, uploaded.getAlpha());
    }
}
