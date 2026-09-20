package dhj.embeddedt.embeddium.impl.render.chunk.metrics;

/**
 * Converts the monotonically increasing work counters that {@code RasterOccluder} publishes (only maintained
 * when {@code AbstractRasterizer.STATS} is set) into per-interval deltas. The GLSM perf report invokes its
 * stats providers once per report interval, so one diff per invocation yields per-interval rates without the
 * provider tracking wall-clock time itself.
 *
 * <p>The first diff after construction returns zeroes: it only establishes the baseline, so a provider
 * registered long after counting started does not report the entire historical total as one interval's rate.
 */
public final class RasterPerfStatsDiffer {
    /**
     * One interval's worth of raster culling work. Counts are sections; nanos are cumulative time spent in
     * the corresponding operation over the interval.
     */
    public record Snapshot(long testedSections, long occludedSections, long testNanos, long occludeNanos) {
        /**
         * {@return average test time per tested section in microseconds, or {@link Double#NaN} when no
         * section was tested this interval}
         */
        public double testMicrosPerSection() {
            return this.testedSections > 0 ? this.testNanos / (this.testedSections * 1_000.0) : Double.NaN;
        }

        /**
         * {@return average occlude time per occluded section in microseconds, or {@link Double#NaN} when no
         * section was occluded this interval}
         */
        public double occludeMicrosPerSection() {
            return this.occludedSections > 0 ? this.occludeNanos / (this.occludedSections * 1_000.0) : Double.NaN;
        }

        /**
         * {@return share of tested sections that ended up occluded, or {@link Double#NaN} when no section
         * was tested this interval}
         */
        public double occludedFraction() {
            return this.testedSections > 0 ? (double) this.occludedSections / this.testedSections : Double.NaN;
        }
    }

    private long lastTestedSections;
    private long lastOccludedSections;
    private long lastTestNanos;
    private long lastOccludeNanos;
    private boolean baselineEstablished;

    /**
     * Diffs the given cumulative counters against the previous invocation. The first invocation establishes
     * the baseline and returns zeroes. Counter resets (values smaller than the remembered ones) likewise
     * re-establish the baseline rather than reporting a negative rate.
     */
    public Snapshot diff(long testedSections, long occludedSections, long testNanos, long occludeNanos) {
        if (!this.baselineEstablished
                || testedSections < this.lastTestedSections
                || occludedSections < this.lastOccludedSections
                || testNanos < this.lastTestNanos
                || occludeNanos < this.lastOccludeNanos) {
            this.lastTestedSections = testedSections;
            this.lastOccludedSections = occludedSections;
            this.lastTestNanos = testNanos;
            this.lastOccludeNanos = occludeNanos;
            this.baselineEstablished = true;
            return new Snapshot(0, 0, 0, 0);
        }

        var snapshot = new Snapshot(
                testedSections - this.lastTestedSections,
                occludedSections - this.lastOccludedSections,
                testNanos - this.lastTestNanos,
                occludeNanos - this.lastOccludeNanos);

        this.lastTestedSections = testedSections;
        this.lastOccludedSections = occludedSections;
        this.lastTestNanos = testNanos;
        this.lastOccludeNanos = occludeNanos;

        return snapshot;
    }
}
