package com.gtnewhorizons.angelica.glsm.debug;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import com.gtnewhorizons.angelica.glsm.ITessellatorData;
import com.gtnewhorizons.angelica.glsm.QuadConverter;
import com.gtnewhorizons.angelica.glsm.streaming.TessellatorStreamingDrawer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GLSMPerfDebugTest {
    @AfterEach
    void disableConfiguredDebug() {
        GLSMPerfDebugHooks.setConfiguredEnabled(false);
    }

    @Test
    void explicitPropertyValueOverridesConfiguredValue() {
        assertTrue(GLSMPerfDebug.resolveEnabled("true", false));
        assertFalse(GLSMPerfDebug.resolveEnabled("false", true));
        assertTrue(GLSMPerfDebug.resolveEnabled(null, true));
        assertFalse(GLSMPerfDebug.resolveEnabled(null, false));
    }

    @Test
    void appliesConfigurationChangesAtRuntime() {
        GLSMPerfDebugHooks.setConfiguredEnabled(false);
        assertFalse(GLSMPerfDebug.isEnabled());

        GLSMPerfDebugHooks.setConfiguredEnabled(true);
        assertTrue(GLSMPerfDebug.isEnabled());

        GLSMPerfDebugHooks.setConfiguredEnabled(false);
        assertFalse(GLSMPerfDebug.isEnabled());
    }

    @Test
    void closesEmptyTessellatorDrawSample() {
        primeNextSample(GLSMPerfDebug.Stage.STREAM_DRAW);
        EmptyTessellatorData tessellator = new EmptyTessellatorData();

        assertEquals(28, TessellatorStreamingDrawer.draw(tessellator));

        assertTrue(tessellator.reset);
        assertFalse(tessellator.drawing);
        assertEquals(1, GLSMPerfDebug.getSampledCount(GLSMPerfDebug.Stage.STREAM_DRAW));
    }

    @Test
    void closesMissingFormatDirectDrawSample() {
        primeNextSample(GLSMPerfDebug.Stage.STREAM_DRAW_DIRECT);
        DirectTessellator tessellator = new DirectTessellator(ByteBuffer.allocateDirect(1));

        TessellatorStreamingDrawer.drawDirect(tessellator);

        assertEquals(1, GLSMPerfDebug.getSampledCount(GLSMPerfDebug.Stage.STREAM_DRAW_DIRECT));
    }

    @Test
    void closesEmptyDirectDrawSample() {
        primeNextSample(GLSMPerfDebug.Stage.STREAM_DRAW_DIRECT);
        DirectTessellator tessellator = new DirectTessellator(ByteBuffer.allocateDirect(1));
        tessellator.setVertexFormat(DefaultVertexFormat.ALL_FORMATS[0]);

        TessellatorStreamingDrawer.drawDirect(tessellator);

        assertEquals(1, GLSMPerfDebug.getSampledCount(GLSMPerfDebug.Stage.STREAM_DRAW_DIRECT));
    }

    @Test
    void closesEmptyQuadElementsSample() {
        primeNextSample(GLSMPerfDebug.Stage.QUAD_ELEMENTS);

        QuadConverter.drawQuadElementsAsTriangles(0, GL11.GL_UNSIGNED_INT, 0L);

        assertEquals(1, GLSMPerfDebug.getSampledCount(GLSMPerfDebug.Stage.QUAD_ELEMENTS));
    }

    @Test
    void accumulatesPersistentFenceReclaimAndQueueMetrics() {
        GLSMPerfDebugHooks.setConfiguredEnabled(false);
        GLSMPerfDebugHooks.setConfiguredEnabled(true);

        GLSMPerfDebug.recordFenceReclaim(128);
        GLSMPerfDebug.recordFenceReclaim(384);
        GLSMPerfDebug.recordFenceQueueDepth(2);
        GLSMPerfDebug.recordFenceQueueDepth(5);
        GLSMPerfDebug.recordFenceQueueDepth(3);

        assertEquals(2, GLSMPerfDebug.getFenceReclaimedRegions());
        assertEquals(512L, GLSMPerfDebug.getFenceReclaimedBytes());
        assertEquals(5, GLSMPerfDebug.getFenceQueuePeak());
    }

    @Test
    void samplesEveryLowFrequencyFenceEvent() {
        GLSMPerfDebugHooks.setConfiguredEnabled(false);
        GLSMPerfDebugHooks.setConfiguredEnabled(true);

        long first = GLSMPerfDebug.begin(GLSMPerfDebug.Stage.STREAM_FENCE_CREATE);
        GLSMPerfDebug.end(GLSMPerfDebug.Stage.STREAM_FENCE_CREATE, first);
        long second = GLSMPerfDebug.begin(GLSMPerfDebug.Stage.STREAM_FENCE_CREATE);
        GLSMPerfDebug.end(GLSMPerfDebug.Stage.STREAM_FENCE_CREATE, second);

        assertEquals(2, GLSMPerfDebug.getSampledCount(GLSMPerfDebug.Stage.STREAM_FENCE_CREATE));
    }

    private static void primeNextSample(GLSMPerfDebug.Stage stage) {
        GLSMPerfDebugHooks.setConfiguredEnabled(false);
        GLSMPerfDebugHooks.setConfiguredEnabled(true);
        for (int i = 0; i < 255; i++) {
            assertEquals(0L, GLSMPerfDebug.begin(stage));
        }
    }

    private static final class EmptyTessellatorData implements ITessellatorData {
        private boolean drawing = true;
        private boolean reset;

        @Override
        public boolean isDrawing() {
            return this.drawing;
        }

        @Override
        public void setDrawing(boolean drawing) {
            this.drawing = drawing;
        }

        @Override
        public int getVertexCount() {
            return 0;
        }

        @Override
        public int[] getRawBuffer() {
            return new int[0];
        }

        @Override
        public int getRawBufferIndex() {
            return 7;
        }

        @Override
        public int getRawBufferSize() {
            return 0;
        }

        @Override
        public void setRawBufferSize(int size) {
            throw new AssertionError("Empty draw must not resize its raw buffer");
        }

        @Override
        public void setRawBuffer(int[] buffer) {
            throw new AssertionError("Empty draw must not replace its raw buffer");
        }

        @Override
        public int getDrawMode() {
            return GL11.GL_QUADS;
        }

        @Override
        public boolean hasTexture() {
            return false;
        }

        @Override
        public boolean hasColor() {
            return false;
        }

        @Override
        public boolean hasNormals() {
            return false;
        }

        @Override
        public boolean hasBrightness() {
            return false;
        }

        @Override
        public void angelica$reset() {
            this.reset = true;
        }
    }
}
