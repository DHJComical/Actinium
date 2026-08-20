package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.DrawCommandList;
import org.embeddedt.embeddium.impl.gl.device.MultiDrawBatch;
import org.embeddedt.embeddium.impl.gl.tessellation.GlIndexType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;

public final class DirectMultiDrawEmitter implements MultiDrawEmitter {
    @Override
    public void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType,
                             MultiDrawBatch batch) {
        int commandCount = batch.size;
        if (commandCount < 0 || commandCount > batch.capacity()) {
            throw new IllegalStateException("MultiDrawBatch command count exceeds its capacity");
        }
        if (commandCount == 0) {
            return;
        }

        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsBaseVertex(batch, primitiveType, GlIndexType.UNSIGNED_INT);
        }
    }
}
