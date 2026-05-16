package com.gtnewhorizons.angelica.glsm.states;

/**
 * Texture binding per texture unit.
 * Ported from Angelica.
 */
public class TextureBinding implements ISettableState<TextureBinding> {
    protected int binding;

    public int getBinding() { return binding; }
    public void setBinding(int binding) { this.binding = binding; }

    @Override
    public TextureBinding set(TextureBinding state) {
        this.binding = state.binding;
        return this;
    }

    @Override
    public boolean sameAs(Object state) {
        if (this == state) return true;
        if (!(state instanceof TextureBinding textureBinding)) return false;
        return binding == textureBinding.binding;
    }

    @Override
    public TextureBinding copy() {
        return new TextureBinding().set(this);
    }
}
