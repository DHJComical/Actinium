package com.gtnewhorizons.angelica.glsm.streaming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TessellatorStreamingDrawerTest {
    @Test
    void restartsRepackCapacityAfterDrawerDestroy() {
        assertEquals(0x10000, TessellatorStreamingDrawer.nextRepackCapacity(0, 96));
    }

    @Test
    void growsRepackCapacityFromTheExistingPowerOfTwo() {
        assertEquals(0x20000, TessellatorStreamingDrawer.nextRepackCapacity(0x10000, 0x10001));
    }
}
