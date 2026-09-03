package org.embeddedt.embeddium.impl.render.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the monotonic frame normalization that shields {@link RenderSectionManager} from
 * out-of-order external frame counters.
 *
 * <p>The terrain pass is driven by the vanilla frame counter while the shadow pass hands in its
 * own counter, which restarts from zero on every shader-pipeline rebuild. Every frame comparison
 * inside the manager (lattice visit stamps, build times, submission frames) assumes one monotonic
 * sequence, so the clock must never move backwards regardless of input order.</p>
 */
class SectionFrameClockTest {
    /**
     * Motivation: the first observed value anchors the sequence; requiring a specific starting
     * point would break worlds joined at an arbitrary vanilla frame count.
     */
    @Test
    void acceptsFirstValueAsIs() {
        assertEquals(0, new SectionFrameClock().next(0));
        assertEquals(42, new SectionFrameClock().next(42));
    }

    /**
     * Motivation: a steadily advancing counter passes through untouched, preserving the exact
     * frame numbers the surrounding code already reasons about.
     */
    @Test
    void followsSteadilyAdvancingInput() {
        var clock = new SectionFrameClock();
        for (int frame = 7; frame < 20; frame++) {
            assertEquals(frame, clock.next(frame));
        }
    }

    /**
     * Motivation: the shadow counter restarting at zero after a pipeline rebuild must not pull
     * the internal sequence backwards; a backwards jump is what poisons the lattice visit
     * stamps and the stale-build filter.
     */
    @Test
    void neverRegressesWhenInputJumpsBackward() {
        var clock = new SectionFrameClock();
        assertEquals(5000, clock.next(5000));
        assertEquals(5001, clock.next(12));
        assertEquals(5002, clock.next(13));
    }

    /**
     * Motivation: the shadow pass and the terrain pass of one game frame hand in the same
     * external number; each caller must still get its own, later frame so "visited this frame"
     * gates cannot confuse the two passes.
     */
    @Test
    void advancesOnRepeatedInput() {
        var clock = new SectionFrameClock();
        assertEquals(10, clock.next(10));
        assertEquals(11, clock.next(10));
        assertEquals(12, clock.next(10));
    }

    /**
     * Motivation: the two production counters interleave per game frame; the output must stay
     * strictly increasing no matter how far the lagging counter trails.
     */
    @Test
    void staysMonotonicAcrossInterleavedCounters() {
        var clock = new SectionFrameClock();
        int previous = clock.next(100);
        for (int i = 0; i < 50; i++) {
            int terrain = clock.next(100 + i);
            int shadow = clock.next(i);
            assertTrue(terrain > previous);
            assertTrue(shadow > terrain);
            previous = shadow;
        }
    }

    /**
     * Motivation: {@code last + 1} overflows at {@link Integer#MAX_VALUE}; the clock must
     * saturate there instead of emitting a negative frame that would regress the sequence.
     */
    @Test
    void saturatesAtMaxValueInsteadOfOverflowing() {
        var clock = new SectionFrameClock();
        assertEquals(Integer.MAX_VALUE, clock.next(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, clock.next(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, clock.next(0));
    }
}
