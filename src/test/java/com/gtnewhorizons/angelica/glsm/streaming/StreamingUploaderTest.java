package com.gtnewhorizons.angelica.glsm.streaming;

import com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Regression tests for the JVM crash where a failed {@code glMapBufferRange} (NULL return) was fed
 * straight into {@code MemoryUtilities.memAddress0}, killing the process with an access violation
 * (hs_err: {@code MemoryUtilities.memAddress0(ByteBuffer)} reading address 0x10). The uploader must
 * instead fall back to {@code bufferData}.
 */
class StreamingUploaderTest {

    @Test
    void failedMappingFallsBackToBufferData() {
        StubRenderBackend backend = new StubRenderBackend();
        backend.mapBufferRangeResult = null; // driver failed the mapping

        ByteBuffer data = ByteBuffer.allocateDirect(64);
        data.limit(64);

        int result = StreamingUploader.upload(backend, StreamingUploader.UploadStrategy.MAP_BUFFER_RANGE, data, 256);

        assertEquals(64, result, "fallback uploads via bufferData and reports the new size");
        assertEquals(1, backend.bufferDataCalls, "must fall back to bufferData instead of dereferencing null");
        assertEquals(data, backend.lastBufferData);
        assertEquals(0, backend.unmapBufferCalls, "nothing was mapped, nothing to unmap");
    }

    @Test
    void successfulMappingCopiesThroughMappedAddressAndUnmaps() {
        StubRenderBackend backend = new StubRenderBackend();
        ByteBuffer mapped = ByteBuffer.allocateDirect(256);
        backend.mapBufferRangeResult = mapped;

        ByteBuffer data = ByteBuffer.allocateDirect(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (i + 1));
        }
        data.flip();

        int result = StreamingUploader.upload(backend, StreamingUploader.UploadStrategy.MAP_BUFFER_RANGE, data, 256);

        assertEquals(256, result, "map path keeps the existing capacity");
        assertEquals(0, backend.bufferDataCalls);
        assertEquals(1, backend.unmapBufferCalls);
        for (int i = 0; i < 64; i++) {
            assertEquals((byte) (i + 1), mapped.get(i), "payload byte " + i + " must reach the mapped buffer");
        }
    }

    @Test
    void oversizedDataReallocatesWithoutMapping() {
        StubRenderBackend backend = new StubRenderBackend();
        backend.mapBufferRangeResult = null; // must not even be consulted on this path

        ByteBuffer data = ByteBuffer.allocateDirect(512);
        data.limit(512);

        int result = StreamingUploader.upload(backend, StreamingUploader.UploadStrategy.MAP_BUFFER_RANGE, data, 128);

        assertEquals(512, result);
        assertEquals(1, backend.bufferDataCalls);
        assertEquals(0, backend.unmapBufferCalls);
    }

    @Test
    void mapBufferRangeAddressReturnsZeroForFailedMapping() {
        StubRenderBackend backend = new StubRenderBackend();
        backend.mapBufferRangeResult = null;

        assertEquals(0L, backend.mapBufferRangeAddress(0, 0, 64, 0));
    }

    @Test
    void mapBufferRangeAddressReturnsRealAddressForSuccessfulMapping() {
        StubRenderBackend backend = new StubRenderBackend();
        ByteBuffer mapped = ByteBuffer.allocateDirect(64);
        backend.mapBufferRangeResult = mapped;

        long address = backend.mapBufferRangeAddress(0, 0, 64, 0);

        assertNotEquals(0L, address);
        assertEquals(MemoryUtilities.memAddress0(mapped), address);
    }
}
