package com.gtnewhorizons.angelica.glsm.stacks;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * BooleanStateStack for per-texture-unit state.
 * Switches to the correct texture unit before enable/disable calls.
 * Ported from Angelica.
 */
public class TextureUnitBooleanStateStack extends BooleanStateStack {
    private final int unitIndex;

    public TextureUnitBooleanStateStack(int glCap, int unitIndex) {
        super(glCap, false);
        this.unitIndex = unitIndex;
    }

    public TextureUnitBooleanStateStack(int glCap, int unitIndex, boolean initialState) {
        super(glCap, initialState);
        this.unitIndex = unitIndex;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled != this.enabled) {
            this.enabled = enabled;
            final int currentUnit = GLStateManager.getActiveTextureUnit();
            if (currentUnit != unitIndex) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unitIndex);
            }
            if (enabled) {
                GL11.glEnable(glCap);
            } else {
                GL11.glDisable(glCap);
            }
            if (currentUnit != unitIndex) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + currentUnit);
            }
        }
    }

    public int getUnitIndex() {
        return unitIndex;
    }
}
