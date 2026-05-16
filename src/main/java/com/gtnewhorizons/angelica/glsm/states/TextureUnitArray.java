package com.gtnewhorizons.angelica.glsm.states;

import com.gtnewhorizons.angelica.glsm.stacks.TextureBindingStack;
import com.gtnewhorizons.angelica.glsm.stacks.TextureUnitBooleanStateStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Array of per-texture-unit state objects.
 * Ported from Angelica.
 */
public class TextureUnitArray {
    private final TextureBindingStack[] bindings;
    private final TextureUnitBooleanStateStack[] states;
    private final TextureUnitBooleanStateStack[] texture1DStates;
    private final TextureUnitBooleanStateStack[] texture3DStates;
    private final TextureUnitBooleanStateStack[] texGenSStates;
    private final TextureUnitBooleanStateStack[] texGenTStates;
    private final TextureUnitBooleanStateStack[] texGenRStates;
    private final TextureUnitBooleanStateStack[] texGenQStates;
    private final TexGenState[] texGenStates;
    private final TexEnvState[] texEnvStates;
    private final Matrix4f[] textureMatrices;

    public TextureUnitArray(int maxTextureUnits) {
        bindings = new TextureBindingStack[maxTextureUnits];
        states = new TextureUnitBooleanStateStack[maxTextureUnits];
        texture1DStates = new TextureUnitBooleanStateStack[maxTextureUnits];
        texture3DStates = new TextureUnitBooleanStateStack[maxTextureUnits];
        texGenSStates = new TextureUnitBooleanStateStack[maxTextureUnits];
        texGenTStates = new TextureUnitBooleanStateStack[maxTextureUnits];
        texGenRStates = new TextureUnitBooleanStateStack[maxTextureUnits];
        texGenQStates = new TextureUnitBooleanStateStack[maxTextureUnits];
        texGenStates = new TexGenState[maxTextureUnits];
        texEnvStates = new TexEnvState[maxTextureUnits];
        textureMatrices = new Matrix4f[maxTextureUnits];

        for (int i = 0; i < maxTextureUnits; i++) {
            bindings[i] = new TextureBindingStack();
            states[i] = new TextureUnitBooleanStateStack(GL11.GL_TEXTURE_2D, i);
            texture1DStates[i] = new TextureUnitBooleanStateStack(GL11.GL_TEXTURE_1D, i);
            texture3DStates[i] = new TextureUnitBooleanStateStack(GL12.GL_TEXTURE_3D, i);
            texGenSStates[i] = new TextureUnitBooleanStateStack(GL11.GL_TEXTURE_GEN_S, i);
            texGenTStates[i] = new TextureUnitBooleanStateStack(GL11.GL_TEXTURE_GEN_T, i);
            texGenRStates[i] = new TextureUnitBooleanStateStack(GL11.GL_TEXTURE_GEN_R, i);
            texGenQStates[i] = new TextureUnitBooleanStateStack(GL11.GL_TEXTURE_GEN_Q, i);
            texGenStates[i] = new TexGenState();
            texEnvStates[i] = new TexEnvState();
            textureMatrices[i] = new Matrix4f().identity();
        }
    }

    public TextureBindingStack getTextureUnitBindings(int index) { return bindings[index]; }
    public TextureUnitBooleanStateStack getTextureUnitStates(int index) { return states[index]; }
    public TextureUnitBooleanStateStack getTexture1DStates(int index) { return texture1DStates[index]; }
    public TextureUnitBooleanStateStack getTexture3DStates(int index) { return texture3DStates[index]; }
    public TextureUnitBooleanStateStack getTexGenSStates(int index) { return texGenSStates[index]; }
    public TextureUnitBooleanStateStack getTexGenTStates(int index) { return texGenTStates[index]; }
    public TextureUnitBooleanStateStack getTexGenRStates(int index) { return texGenRStates[index]; }
    public TextureUnitBooleanStateStack getTexGenQStates(int index) { return texGenQStates[index]; }
    public TexGenState getTexGenState(int index) { return texGenStates[index]; }
    public TexEnvState getTexEnvState(int index) { return texEnvStates[index]; }
    public Matrix4f getTextureUnitMatrix(int index) { return textureMatrices[index]; }
}
