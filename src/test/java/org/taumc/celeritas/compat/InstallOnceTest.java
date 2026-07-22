package org.taumc.celeritas.compat;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallOnceTest {
    @Test
    void runsSuccessfulInstallationOnlyOnce() {
        InstallOnce guard = new InstallOnce();
        AtomicInteger calls = new AtomicInteger();
        assertTrue(guard.run(calls::incrementAndGet));
        assertFalse(guard.run(calls::incrementAndGet));
        assertEquals(1, calls.get());
    }

    @Test
    void allowsRetryAfterFailedInstallation() {
        InstallOnce guard = new InstallOnce();
        assertThrows(IllegalStateException.class, () -> guard.run(() -> {
            throw new IllegalStateException("registration failed");
        }));
        assertTrue(guard.run(() -> { }));
    }
}
