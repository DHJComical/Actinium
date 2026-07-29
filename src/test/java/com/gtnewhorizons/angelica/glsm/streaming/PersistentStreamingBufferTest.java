package com.gtnewhorizons.angelica.glsm.streaming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentStreamingBufferTest {
    @Test
    void batchesPendingBytesByRingQuarter() {
        int capacity = 16 * 1024;

        assertFalse(PersistentStreamingBuffer.shouldCreateFence(0, capacity, false));
        assertFalse(PersistentStreamingBuffer.shouldCreateFence(4095, capacity, false));
        assertTrue(PersistentStreamingBuffer.shouldCreateFence(4096, capacity, false));
    }

    @Test
    void capacityPressureRequestsAPartialBatchFence() {
        assertTrue(PersistentStreamingBuffer.shouldCreateFence(1, 16 * 1024, true));
        assertFalse(PersistentStreamingBuffer.shouldCreateFence(0, 16 * 1024, true));
    }
}
