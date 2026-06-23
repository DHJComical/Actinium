package org.embeddedt.embeddium.impl.gl.shader.uniform;

import com.mitchej123.lwjgl.GL32;
import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

import org.embeddedt.embeddium.impl.gl.buffer.GlBuffer;

public class GlUniformBlock {
    private final int binding;

    public GlUniformBlock(int uniformBlockBinding) {
        this.binding = uniformBlockBinding;
    }

    public void bindBuffer(GlBuffer buffer) {
        LWJGL.glBindBufferBase(GL32.GL_UNIFORM_BUFFER, this.binding, buffer.handle());
    }
}

