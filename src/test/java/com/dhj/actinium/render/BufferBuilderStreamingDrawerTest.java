package com.dhj.actinium.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BufferBuilderStreamingDrawerTest {
    @Test
    void persistentDrawUsesItsVaoAndVboWithoutChangingTheArrayBufferBinding() {
        BufferBuilderStreamingDrawer.DrawPath drawPath = BufferBuilderStreamingDrawer.DrawPath.fromFirstVertex(0);

        assertEquals(BufferBuilderStreamingDrawer.DrawPath.PERSISTENT, drawPath);
        assertEquals(101, drawPath.select(101, 201));
        assertEquals(102, drawPath.select(102, 202));
        assertFalse(drawPath.changesArrayBufferBinding());
    }

    @Test
    void orphanDrawBindsItsUploadVboAndRequiresCallerBindingRestoration() {
        BufferBuilderStreamingDrawer.DrawPath drawPath = BufferBuilderStreamingDrawer.DrawPath.fromFirstVertex(-1);

        assertEquals(BufferBuilderStreamingDrawer.DrawPath.ORPHAN, drawPath);
        assertEquals(201, drawPath.select(101, 201));
        assertEquals(202, drawPath.select(102, 202));
        assertTrue(drawPath.changesArrayBufferBinding());
    }
}
