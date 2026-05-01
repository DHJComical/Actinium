package org.embeddedt.embeddium.impl.gl.shader.uniform;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public class GlUniformInt3v extends GlUniform<int[]> {
    public GlUniformInt3v(int index) {
        super(index);
    }

    @Override
    public void set(int[] value) {
        if (value.length != 3) {
            throw new IllegalArgumentException("value.length != 3");
        }

        this.set(value[0], value[1], value[2]);
    }

    public void set(int x, int y, int z) {
        LWJGL.glUniform3i(this.index, x, y, z);
    }
}
