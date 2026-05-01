package org.embeddedt.embeddium.impl.gl.shader.uniform;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import org.joml.Matrix3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class GlUniformMatrix3f extends GlUniform<Matrix3f> {
    private final FloatBuffer scratchBuffer = BufferUtils.createFloatBuffer(12);

    public GlUniformMatrix3f(int index) {
        super(index);
    }

    @Override
    public void set(Matrix3f value) {
        this.scratchBuffer.clear();
        value.get(this.scratchBuffer);
        LWJGL.glUniformMatrix3fv(this.index, false, this.scratchBuffer);
    }
}
