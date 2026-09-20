package dhj.embeddedt.embeddium.impl.render.chunk.compile;

import dhj.embeddedt.embeddium.impl.common.util.NativeBuffer;
import dhj.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;

public class ChunkBuildContext {
    public final ChunkBuildBuffers buffers;

    public ChunkBuildContext(RenderPassConfiguration renderPassConfiguration) {
        this.buffers = new ChunkBuildBuffers(renderPassConfiguration);
    }

    /**
     * Releases the per-task scratch state after a build job finished. The off-heap mesh buffers
     * keep their capacity so the next task does not pay for re-growing them from scratch.
     */
    public void cleanup() {
        this.buffers.resetForTask();
    }

    /**
     * Frees the retained off-heap mesh buffers for good. Called only when this context is
     * discarded (executor shutdown); buffers owned by contexts that never reach this point are
     * still covered by the reclaim queue of {@link NativeBuffer}.
     */
    public void destroy() {
        this.buffers.destroy();
    }
}
