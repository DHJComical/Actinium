package com.gtnewhorizons.angelica.glsm.recording;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CommandBufferSecondaryColorTest {

    @Test
    void secondaryColorRoundTripsThroughTheBuffer() {
        final CommandBuffer buf = new CommandBuffer();
        buf.writeSecondaryColor(0.25f, 0.5f, 1.0f);

        buf.resetRead();
        assertEquals(GLCommand.SECONDARY_COLOR, buf.readInt());
        assertEquals(0.25f, buf.readFloat());
        assertEquals(0.5f, buf.readFloat());
        assertEquals(1.0f, buf.readFloat());
        assertFalse(buf.hasRemaining());
    }

    @Test
    void secondaryColorLayoutIsSixteenBytes() {
        assertEquals(16, GLCommand.getCommandSize(GLCommand.SECONDARY_COLOR, 0));

        final CommandBuffer buf = new CommandBuffer();
        buf.writeSecondaryColor(1.0f, 0.0f, 0.5f);
        assertEquals(16, buf.size());
    }

    @Test
    void secondaryColorHasAStableDebugName() {
        assertEquals("SECONDARY_COLOR", GLCommand.getName(GLCommand.SECONDARY_COLOR));
    }
}
