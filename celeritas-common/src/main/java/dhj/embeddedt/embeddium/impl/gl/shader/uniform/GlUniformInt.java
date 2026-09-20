package dhj.embeddedt.embeddium.impl.gl.shader.uniform;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;


public class GlUniformInt extends GlUniform<Integer> {
    public GlUniformInt(int index) {
        super(index);
    }

    @Override
    public void set(Integer value) {
        this.setInt(value);
    }

    public void setInt(int value) {
        LWJGL.glUniform1i(this.index, value);
    }
}

