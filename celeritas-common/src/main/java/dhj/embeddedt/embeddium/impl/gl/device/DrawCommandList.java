package dhj.embeddedt.embeddium.impl.gl.device;

import dhj.embeddedt.embeddium.impl.gl.buffer.GlBuffer;
import dhj.embeddedt.embeddium.impl.gl.tessellation.GlIndexType;
import dhj.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;

public interface DrawCommandList extends AutoCloseable {
    void multiDrawElementsBaseVertex(DirectMultiDrawBatch batch, GlPrimitiveType primitiveType, GlIndexType indexType);

    void multiDrawElementsIndirect(GlBuffer indirectBuffer, int count, GlPrimitiveType primitiveType, GlIndexType indexType);

    void endTessellating();

    void flush();

    @Override
    default void close() {
        this.flush();
    }
}
