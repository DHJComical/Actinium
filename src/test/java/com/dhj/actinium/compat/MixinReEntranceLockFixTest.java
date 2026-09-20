package com.dhj.actinium.compat;

import org.junit.jupiter.api.Test;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.util.ReEntranceLock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Guards the public Mixin service contract used by {@link MixinReEntranceLockFix}.
 */
class MixinReEntranceLockFixTest {

    @Test
    void drainPopsLeakedDepthBackToZero() {
        ReEntranceLock lock = new ReEntranceLock(1);
        lock.push(); // outer lockAndSelect
        lock.push(); // nested lockAndSelect that throws without popping
        lock.pop();  // outer call pops once, leaving the leaked depth of 1
        assertEquals(1, lock.getDepth());
        MixinReEntranceLockFix.drain(lock);
        assertEquals(0, lock.getDepth());
    }

    /**
     * The preload diagnostic must report the tracker's answer verbatim (that is the condition
     * {@code MixinInfo.readDeclaredTargets} aborts on) and must never turn an unavailable
     * tracker into a "not loaded" answer.
     */
    @Test
    void classTrackerProbeKeepsThreeDistinctAnswers() {
        assertEquals(Boolean.TRUE, MixinReEntranceLockFix.isClassLoaded(new StubClassTracker(true), "a.B"));
        assertEquals(Boolean.FALSE, MixinReEntranceLockFix.isClassLoaded(new StubClassTracker(false), "a.B"));
        assertNull(MixinReEntranceLockFix.isClassLoaded(null, "a.B"));
    }

    /** Minimal tracker double: the probe only consumes the loaded flag. */
    private static final class StubClassTracker implements IClassTracker {
        private final boolean loaded;

        private StubClassTracker(boolean loaded) {
            this.loaded = loaded;
        }

        @Override
        public boolean isClassLoaded(String className) {
            return this.loaded;
        }

        @Override
        public String getClassRestrictions(String className) {
            return "";
        }

        @Override
        public void registerInvalidClass(String className) {
        }
    }
}
