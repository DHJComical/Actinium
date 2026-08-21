package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferUsage;
import org.embeddedt.embeddium.impl.gl.buffer.GlMutableBuffer;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.DrawCommandList;
import org.embeddedt.embeddium.impl.gl.device.MultiDrawBatch;
import org.embeddedt.embeddium.impl.gl.tessellation.GlIndexType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;
import com.mitchej123.lwjgl.LWJGLServiceProvider;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

/**
 * A multidraw emitter that uses indirect rendering to exploit hardware acceleration, which
 * reduces CPU overhead on some platforms.
 * @author Ven
 */
public class IndirectMultiDrawEmitter implements MultiDrawEmitter {
    // uint  count;
    // uint  instanceCount;
    // uint  firstIndex;
    // int  baseVertex;
    // uint  baseInstance;
    private static final int COMMAND_SIZE = 4 * 5;
    private static final int BUFFER_SIZE = MultiDrawEmitter.MAX_COMMAND_COUNT * COMMAND_SIZE;

    private final long indirectBuffer;
    private final GlMutableBuffer indirectBufferGpu;

    public IndirectMultiDrawEmitter() {
        this.indirectBuffer = LWJGL.nmemAlignedAlloc(32, BUFFER_SIZE);
        if (this.indirectBuffer == LWJGLServiceProvider.NULL) {
            throw new OutOfMemoryError("Failed to allocate indirect buffer");
        }
        this.prefillConstants();
        this.indirectBufferGpu = new GlMutableBuffer();
    }

    private void prefillConstants() {
        long ptr = this.indirectBuffer;
        for (int i = 0; i < MultiDrawEmitter.MAX_COMMAND_COUNT; i++) {
            LWJGL.memPutInt(ptr + 4L, 1); // instanceCount
            LWJGL.memPutInt(ptr + 16L, 0); // baseInstance
            ptr += COMMAND_SIZE;
        }
    }

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
        if (commandCount > MultiDrawEmitter.MAX_COMMAND_COUNT) {
            throw new IllegalStateException("MultiDrawBatch command count exceeds indirect buffer capacity");
        }

        this.buildCommands(batch, commandCount);
        this.uploadAndDraw(commandList, tessellation, primitiveType, commandCount);
    }

    private void uploadAndDraw(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType,
                               int commandCount) {
        commandList.uploadData(this.indirectBufferGpu, this.indirectBuffer, (long) commandCount * COMMAND_SIZE,
                GlBufferUsage.STREAM_DRAW);

        commandList.bindBuffer(GlBufferTarget.DRAW_INDIRECT_BUFFER, this.indirectBufferGpu);
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsIndirect(this.indirectBufferGpu, commandCount, primitiveType,
                    GlIndexType.UNSIGNED_INT);
        }
        commandList.bindBuffer(GlBufferTarget.DRAW_INDIRECT_BUFFER, null);
    }

    /**
     * Converts shared direct-draw command arrays into this emitter's native indirect command layout.
     */
    private void buildCommands(MultiDrawBatch batch, int commandCount) {
        long pBaseVertex = batch.pBaseVertex;
        long pElementCount = batch.pElementCount;
        long pElementPointer = batch.pElementPointer;
        long pointer = this.indirectBuffer;
        int pointerSize = LWJGL.getPointerSize();

        for (int i = 0; i < commandCount; i++) {
            LWJGL.memPutInt(pointer,
                    LWJGL.memGetInt(pElementCount + ((long) i * Integer.BYTES))); // count
            LWJGL.memPutInt(pointer + 8L,
                    (int) (LWJGL.memGetAddress(pElementPointer + ((long) i * pointerSize)) / 4)); // firstIndex
            LWJGL.memPutInt(pointer + 12L,
                    LWJGL.memGetInt(pBaseVertex + ((long) i * Integer.BYTES))); // baseVertex
            pointer += COMMAND_SIZE;
        }
    }

    @Override
    public void delete() {
        LWJGL.nmemAlignedFree(this.indirectBuffer);
        this.indirectBufferGpu.delete();
    }
}
