package dhj.embeddedt.embeddium.api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the rounding behaviour of the packed-colour mixing helpers. The previous implementation shifted the
 * intermediate 16-bit products right by 8 without a rounding term, which truncated every component towards zero and
 * darkened channels even when multiplying by full white.
 */
class ColorMixerTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MID_GREY = 0xFF808080;

    @Test
    void mulByWhiteIsIdentity() {
        assertEquals(WHITE, ColorMixer.mul(WHITE, WHITE));
        assertEquals(MID_GREY, ColorMixer.mul(MID_GREY, WHITE));
    }

    @Test
    void mulRoundsToNearestInsteadOfTruncating() {
        // 255 * 1 == 255, so the correctly rounded result is 1; truncation would produce 0 and lose the component.
        assertEquals(0xFF010101, ColorMixer.mul(WHITE, 0xFF010101));
    }

    @Test
    void mulSingleWithoutAlphaByFullScaleIsIdentityAndKeepsAlpha() {
        assertEquals(MID_GREY, ColorMixer.mulSingleWithoutAlpha(MID_GREY, 255));
        assertEquals(0x7F808080, ColorMixer.mulSingleWithoutAlpha(0x7F808080, 255));
    }

    @Test
    void mulSingleWithoutAlphaRoundsToNearestInsteadOfTruncating() {
        assertEquals(0xFF010101, ColorMixer.mulSingleWithoutAlpha(WHITE, 1));
    }

    @Test
    void mixRoundsToNearestInsteadOfTruncating() {
        // Half way between white and black is 127.5 per component, which must round to 128 rather than 127.
        assertEquals(0x00808080, ColorMixer.mix(0x00FFFFFF, 0x00000000, 0.5f));
    }
}
