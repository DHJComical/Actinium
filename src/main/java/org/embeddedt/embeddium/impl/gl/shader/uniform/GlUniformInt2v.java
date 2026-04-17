package org.embeddedt.embeddium.impl.gl.shader.uniform;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public class GlUniformInt2v extends GlUniform<int[]> {
    public GlUniformInt2v(int index) {
        super(index);
    }

    @Override
    public void set(int[] value) {
        if (value.length != 2) {
            throw new IllegalArgumentException("value.length != 2");
        }

        this.set(value[0], value[1]);
    }

    public void set(int x, int y) {
        LWJGL.glUniform2i(this.index, x, y);
    }
}
