package dhj.embeddedt.embeddium.impl.render.chunk.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RasterPerfStatsDifferTest {
    @Test
    void firstDiffEstablishesBaselineWithoutReportingHistory() {
        RasterPerfStatsDiffer differ = new RasterPerfStatsDiffer();

        var snapshot = differ.diff(1000, 200, 5_000_000, 900_000);

        assertEquals(0, snapshot.testedSections());
        assertEquals(0, snapshot.occludedSections());
        assertEquals(0, snapshot.testNanos());
        assertEquals(0, snapshot.occludeNanos());
    }

    @Test
    void diffsAgainstPreviousInvocation() {
        RasterPerfStatsDiffer differ = new RasterPerfStatsDiffer();
        differ.diff(1000, 200, 5_000_000, 900_000);

        var snapshot = differ.diff(1300, 260, 6_500_000, 1_200_000);

        assertEquals(300, snapshot.testedSections());
        assertEquals(60, snapshot.occludedSections());
        assertEquals(1_500_000, snapshot.testNanos());
        assertEquals(300_000, snapshot.occludeNanos());
    }

    @Test
    void computesPerSectionAveragesAndOccludedFraction() {
        var snapshot = new RasterPerfStatsDiffer.Snapshot(300, 60, 1_500_000, 300_000);

        assertEquals(5.0, snapshot.testMicrosPerSection(), 1e-9);
        assertEquals(5.0, snapshot.occludeMicrosPerSection(), 1e-9);
        assertEquals(0.2, snapshot.occludedFraction(), 1e-9);
    }

    @Test
    void averagesAreNaNWhenNothingWasCounted() {
        var snapshot = new RasterPerfStatsDiffer.Snapshot(0, 0, 0, 0);

        assertTrue(Double.isNaN(snapshot.testMicrosPerSection()));
        assertTrue(Double.isNaN(snapshot.occludeMicrosPerSection()));
        assertTrue(Double.isNaN(snapshot.occludedFraction()));
    }

    @Test
    void counterResetReEstablishesBaselineInsteadOfReportingNegativeRates() {
        RasterPerfStatsDiffer differ = new RasterPerfStatsDiffer();
        differ.diff(1000, 200, 5_000_000, 900_000);

        var afterReset = differ.diff(10, 2, 50_000, 9_000);

        assertEquals(0, afterReset.testedSections());
        assertEquals(0, afterReset.occludedSections());

        var next = differ.diff(30, 7, 110_000, 19_000);
        assertEquals(20, next.testedSections());
        assertEquals(5, next.occludedSections());
        assertEquals(60_000, next.testNanos());
        assertEquals(10_000, next.occludeNanos());
    }
}
