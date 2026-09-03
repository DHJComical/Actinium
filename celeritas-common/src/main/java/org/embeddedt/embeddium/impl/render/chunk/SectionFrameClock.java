package org.embeddedt.embeddium.impl.render.chunk;

/**
 * Normalizes the external frame numbers handed to {@link RenderSectionManager} into one
 * strictly increasing sequence.
 *
 * <p>The terrain pass and the shadow pass are driven by two independent external counters: the
 * vanilla frame counter, and the shadow renderer's own counter which restarts from zero whenever
 * the shader pipeline is rebuilt. The manager archives and compares frame numbers in several
 * places — lattice visit stamps, {@code lastUpdatedFrame}, build times and the per-section
 * submission/build frames — and every comparison assumes a single monotonic sequence. A counter
 * that jumps backwards poisons those comparisons: searches skip cells carrying a stamp from the
 * "future", and finished build results are discarded as stale without releasing their
 * cancellation token.</p>
 *
 * <p>Feeding every external frame through this clock keeps the internal sequence monotonic while
 * preserving caller order within a game frame: the first caller takes the next number and later
 * callers of the same frame take higher numbers. Not thread-safe; the section manager only calls
 * it from the render thread.</p>
 */
final class SectionFrameClock {
    private boolean initialized;
    private int last;

    /**
     * Returns the internal frame number for {@code external}. The first observed value is
     * accepted as-is; afterwards the result is {@code max(external, previous + 1)}, so repeated,
     * out-of-order or restarted external counters still yield a strictly advancing sequence.
     * Saturates at {@link Integer#MAX_VALUE} rather than overflowing into negative frames.
     */
    int next(int external) {
        if (!this.initialized) {
            this.initialized = true;
        } else {
            int successor = this.last == Integer.MAX_VALUE ? Integer.MAX_VALUE : this.last + 1;
            external = Math.max(external, successor);
        }
        this.last = external;
        return external;
    }
}
