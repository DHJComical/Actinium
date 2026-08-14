package com.gtnewhorizons.angelica.glsm.ffp;

import com.gtnewhorizons.angelica.glsm.states.Color4;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that the GLSM current-color dirty sentinel never leaks into FFP shader
 * uniforms. {@code GLStateManager.clearCurrentColor()} (the redirect target of
 * vanilla {@code GlStateManager.resetColor()}) marks the cached color with
 * (-1,-1,-1,-1); uploading that raw value makes every vertex-color-less draw
 * fully transparent (negative alpha clamps to 0 in GLSL).
 *
 * <p>GLStateManager itself cannot be loaded without a GL context (its static
 * initializers query the backend), so the sentinel state is reproduced directly.
 */
class UniformColorSanitizeTest {

    @Test
    void dirtySentinelColorIsNormalizedToOpaqueWhite() {
        Color4 result = Uniforms.sanitizeUniformColor(new Color4(-1.0F, -1.0F, -1.0F, -1.0F));

        assertEquals(1.0F, result.getRed());
        assertEquals(1.0F, result.getGreen());
        assertEquals(1.0F, result.getBlue());
        assertEquals(1.0F, result.getAlpha());
    }

    @Test
    void partiallyNegativeSentinelIsNormalizedToOpaqueWhite() {
        Color4 result = Uniforms.sanitizeUniformColor(new Color4(1.0F, 1.0F, 1.0F, -1.0F));

        assertEquals(1.0F, result.getAlpha());
        assertEquals(1.0F, result.getRed());
    }

    @Test
    void zeroAlphaColorIsNotTreatedAsDirtySentinel() {
        Color4 result = Uniforms.sanitizeUniformColor(new Color4(0.0F, 0.0F, 0.0F, 0.0F));

        assertSame(result, Uniforms.sanitizeUniformColor(result));
        assertEquals(0.0F, result.getAlpha());
    }

    @Test
    void validColorPassesThroughUnchanged() {
        Color4 color = new Color4(0.5F, 0.5F, 0.5F, 0.5F);

        assertSame(color, Uniforms.sanitizeUniformColor(color));
    }
}
