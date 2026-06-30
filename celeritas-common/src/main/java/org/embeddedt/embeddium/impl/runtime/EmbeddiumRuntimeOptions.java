package org.embeddedt.embeddium.impl.runtime;

import org.embeddedt.embeddium.impl.render.chunk.MultiDrawMode;

import java.util.function.Supplier;

public final class EmbeddiumRuntimeOptions {
    private static volatile Supplier<MultiDrawMode> chunkMultiDrawMode = () -> MultiDrawMode.DIRECT;

    private EmbeddiumRuntimeOptions() {
    }

    public static MultiDrawMode chunkMultiDrawMode() {
        return chunkMultiDrawMode.get();
    }

    public static void setChunkMultiDrawMode(Supplier<MultiDrawMode> supplier) {
        chunkMultiDrawMode = supplier != null ? supplier : () -> MultiDrawMode.DIRECT;
    }
}
