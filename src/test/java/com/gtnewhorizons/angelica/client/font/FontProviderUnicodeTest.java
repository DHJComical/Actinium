package com.gtnewhorizons.angelica.client.font;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FontProviderUnicodeTest {

    @Test
    void readFullyFillsBufferAcrossShortReads() throws Exception {
        byte[] data = new byte[65536];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        InputStream shortReads = new InputStream() {
            private int pos;

            @Override
            public int read() {
                return pos < data.length ? data[pos++] & 0xFF : -1;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (pos >= data.length) {
                    return -1;
                }
                int n = Math.min(len, Math.min(997, data.length - pos));
                System.arraycopy(data, pos, b, off, n);
                pos += n;
                return n;
            }
        };

        byte[] buf = new byte[data.length];
        int total = FontProviderUnicode.readFully(shortReads, buf);
        assertEquals(data.length, total);
        assertArrayEquals(data, buf);
    }

    @Test
    void readFullyStopsAtEndOfStream() throws Exception {
        byte[] buf = new byte[16];
        int total = FontProviderUnicode.readFully(InputStream.nullInputStream(), buf);
        assertEquals(0, total);
    }
}
