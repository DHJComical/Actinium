package com.gtnewhorizon.gtnhlib.client.renderer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class TessellatorManagerTest {

    @Test
    void captureStackIsIsolatedPerThread() throws Exception {
        TessellatorManager.startCapturingDirect();
        try {
            AtomicBoolean otherThreadSeesCapture = new AtomicBoolean(true);
            AtomicBoolean otherThreadBalancedAfterOwnCapture = new AtomicBoolean(false);
            Thread other = new Thread(() -> {
                otherThreadSeesCapture.set(TessellatorManager.shouldInterceptBufferBuilderDraw());
                TessellatorManager.startCapturingDirect();
                TessellatorManager.stopCapturingDirect();
                otherThreadBalancedAfterOwnCapture.set(!TessellatorManager.shouldInterceptBufferBuilderDraw());
            });
            other.start();
            other.join();

            assertFalse(otherThreadSeesCapture.get(), "another thread must not see this thread's capture");
            assertTrue(otherThreadBalancedAfterOwnCapture.get(), "other thread must balance its own capture");
            assertTrue(TessellatorManager.shouldInterceptBufferBuilderDraw(), "this thread's capture must stay armed");
        } finally {
            TessellatorManager.stopCapturingDirect();
        }
        assertFalse(TessellatorManager.shouldInterceptBufferBuilderDraw());
    }
}
