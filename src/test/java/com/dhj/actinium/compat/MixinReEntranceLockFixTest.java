package com.dhj.actinium.compat;

import org.junit.jupiter.api.Test;
import org.spongepowered.asm.util.ReEntranceLock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void clearInvalidVanillaClassesNeverThrowsOutsideLaunchEnvironment() {
        assertDoesNotThrow(MixinReEntranceLockFix::clearInvalidVanillaClasses);
    }

    @Test
    void preloadClassesAcceptsClassReferences() {
        assertDoesNotThrow(() -> MixinReEntranceLockFix.preloadClasses(Object.class));
    }

    @Test
    void clearLeakedLockNeverThrowsOutsideLaunchEnvironment() {
        assertDoesNotThrow(MixinReEntranceLockFix::clearLeakedLock);
    }
}
