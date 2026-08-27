package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.DirectMultiDrawBatch;
import org.embeddedt.embeddium.impl.gl.device.DrawCommandList;
import org.embeddedt.embeddium.impl.gl.tessellation.GlIndexType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

/**
 * Executes a direct multidraw batch as individual glDrawElementsBaseVertex calls. Used for sections
 * with an active animation (each needs its own model offset) and for the INDIVIDUAL multidraw mode.
 */
public final class IndividualDrawEmitter {
    public void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType,
                             DirectMultiDrawBatch batch) {
        int commandCount = batch.size();
        if (commandCount < 0 || commandCount > batch.capacity()) {
            throw new IllegalStateException("MultiDrawBatch command count exceeds its capacity");
        }
        if (commandCount == 0) {
            return;
        }

        try (DrawCommandList ignored = commandList.beginTessellating(tessellation)) {
            final int mode = primitiveType.getId();
            final int type = GlIndexType.UNSIGNED_INT.getFormatId();
            final int pointerSize = LWJGL.getPointerSize();

            for (int i = 0; i < commandCount; i++) {
                final int count = LWJGL.memGetInt(batch.pElementCount + (long) i * Integer.BYTES);
                if (count > 0) {
                    LWJGL.glDrawElementsBaseVertex(
                        mode,
                        count,
                        type,
                        LWJGL.memGetAddress(batch.pElementPointer + (long) i * pointerSize),
                        LWJGL.memGetInt(batch.pBaseVertex + (long) i * Integer.BYTES)
                    );
                }
            }
        }
    }
}
