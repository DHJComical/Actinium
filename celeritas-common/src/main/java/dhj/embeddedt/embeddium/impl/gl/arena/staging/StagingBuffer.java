package dhj.embeddedt.embeddium.impl.gl.arena.staging;

import dhj.embeddedt.embeddium.impl.gl.buffer.GlBuffer;
import dhj.embeddedt.embeddium.impl.gl.device.CommandList;

import java.nio.ByteBuffer;

public interface StagingBuffer {
    void enqueueCopy(CommandList commandList, ByteBuffer data, GlBuffer dst, long writeOffset);

    void flush(CommandList commandList);

    void delete(CommandList commandList);

    void flip();
}
