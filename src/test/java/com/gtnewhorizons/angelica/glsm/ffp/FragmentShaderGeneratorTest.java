package com.gtnewhorizons.angelica.glsm.ffp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FragmentShaderGeneratorTest {
    private static final int GLOBAL_BITS = 10;
    private static final int BIT_NR_ENABLED_UNITS = 7;
    private static final int U_ENABLED = 0;
    private static final int U_MODE = 1;
    private static final int U_COMBINE_RGB = 5;
    private static final int U_COMBINE_ALPHA = 9;
    private static final int U_ARG0_RGB = 17;
    private static final int U_ARG1_RGB = 22;
    private static final int U_ARG0_ALPHA = 32;
    private static final int U_ARG1_ALPHA = 37;

    @Test
    void combineSourcesCanReferenceSpecificEnabledTextureUnits() {
        FragmentKey key = FragmentKey.fromPacked(new long[] {
            packGlobal(2) | (packCombineUnit(
                FragmentKey.SRC_TEXTURE0,
                FragmentKey.SRC_TEXTURE1,
                FragmentKey.SRC_TEXTURE0,
                FragmentKey.SRC_TEXTURE1
            ) << GLOBAL_BITS),
            packModulateUnit()
        }, 2);

        String shader = FragmentShaderGenerator.generate(key);

        assertTrue(shader.contains("argRgb0_0 = tex0Color.rgb;"), shader);
        assertTrue(shader.contains("argRgb1_0 = tex1Color.rgb;"), shader);
        assertTrue(shader.contains("argAlpha0_0 = tex0Color.a;"), shader);
        assertTrue(shader.contains("argAlpha1_0 = tex1Color.a;"), shader);
    }

    @Test
    void disabledSpecificTextureUnitSourceFallsBackToPreviousColor() {
        FragmentKey key = FragmentKey.fromPacked(new long[] {
            packGlobal(1) | (packCombineUnit(
                FragmentKey.SRC_TEXTURE1,
                FragmentKey.SRC_TEXTURE0,
                FragmentKey.SRC_TEXTURE1,
                FragmentKey.SRC_TEXTURE0
            ) << GLOBAL_BITS)
        }, 1);

        String shader = FragmentShaderGenerator.generate(key);

        assertTrue(shader.contains("argRgb0_0 = v_Color.rgb;"), shader);
        assertTrue(shader.contains("argAlpha0_0 = v_Color.a;"), shader);
        assertFalse(shader.contains("tex1Color"), shader);
    }

    private static long packGlobal(int nrEnabledUnits) {
        return (long) nrEnabledUnits << BIT_NR_ENABLED_UNITS;
    }

    private static long packCombineUnit(int rgbSource0, int rgbSource1, int alphaSource0, int alphaSource1) {
        long bits = 1L << U_ENABLED;
        bits |= (long) FragmentKey.TEX_ENV_COMBINE << U_MODE;
        bits |= (long) FragmentKey.COMBINE_MODULATE << U_COMBINE_RGB;
        bits |= (long) FragmentKey.COMBINE_MODULATE << U_COMBINE_ALPHA;
        bits |= packArg(rgbSource0, FragmentKey.OP_SRC_COLOR) << U_ARG0_RGB;
        bits |= packArg(rgbSource1, FragmentKey.OP_SRC_COLOR) << U_ARG1_RGB;
        bits |= packArg(alphaSource0, FragmentKey.OP_SRC_ALPHA) << U_ARG0_ALPHA;
        bits |= packArg(alphaSource1, FragmentKey.OP_SRC_ALPHA) << U_ARG1_ALPHA;
        return bits;
    }

    private static long packModulateUnit() {
        long bits = 1L << U_ENABLED;
        bits |= (long) FragmentKey.TEX_ENV_MODULATE << U_MODE;
        return bits;
    }

    private static long packArg(int source, int operand) {
        return source | (long) operand << 3;
    }
}
