package org.embeddedt.embeddium.impl.gl.shader.uniform;

import com.mitchej123.lwjgl.GL30;
import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

import org.joml.Matrix4fc;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class GlUniformMatrix4f extends GlUniform<Matrix4fc>  {
    private final FloatBuffer scratchBuffer = BufferUtils.createFloatBuffer(16);

    public GlUniformMatrix4f(int index) {
        super(index);
    }

    @Override
    public void set(Matrix4fc value) {
        this.scratchBuffer.clear();
        value.get(this.scratchBuffer);
        LWJGL.glUniformMatrix4fv(this.index, false, this.scratchBuffer);
    }
}

