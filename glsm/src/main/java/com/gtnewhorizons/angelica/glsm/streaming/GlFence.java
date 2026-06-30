package com.gtnewhorizons.angelica.glsm.streaming;

import com.mitchej123.lwjgl.GL32;
import com.mitchej123.lwjgl.MemoryStack;

import java.nio.IntBuffer;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

public class GlFence {
    private final long id;
    private boolean disposed;

    public GlFence(long id) {
        this.id = id;
    }

    public boolean isCompleted() {
        this.checkDisposed();

        int result;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.callocInt(1);
            result = LWJGL.glGetSynci(this.id, GL32.GL_SYNC_STATUS, count);

            if (count.get(0) != 1) {
                throw new RuntimeException("glGetSync returned more than one value");
            }
        }

        return result == GL32.GL_SIGNALED;
    }

    public void sync() {
        this.checkDisposed();
        this.sync(Long.MAX_VALUE);
    }

    public void sync(long timeout) {
        this.checkDisposed();
        int result = LWJGL.glClientWaitSync(this.id, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, timeout);

        if (result == GL32.GL_WAIT_FAILED) {
            throw new RuntimeException("glClientWaitSync failed");
        }

        if (result == GL32.GL_TIMEOUT_EXPIRED) {
            throw new RuntimeException("Timed out while waiting for GL fence");
        }
    }

    public void delete() {
        LWJGL.glDeleteSync(this.id);
        this.disposed = true;
    }

    private void checkDisposed() {
        if (this.disposed) {
            throw new IllegalStateException("Fence object has been disposed");
        }
    }
}
