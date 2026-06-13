package org.embeddedt.embeddium.impl.gl.shader.uniform;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public class GlUniformInt4v extends GlUniform<int[]> {
    public GlUniformInt4v(int index) {
        super(index);
    }

    @Override
    public void set(int[] value) {
        if (value.length != 4) {
            throw new IllegalArgumentException("value.length != 4");
        }

        this.set(value[0], value[1], value[2], value[3]);
    }

    public void set(int x, int y, int z, int w) {
        LWJGL.glUniform4i(this.index, x, y, z, w);
    }
}
