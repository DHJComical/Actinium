package com.gtnewhorizon.gtnhlib.client.renderer;

import com.gtnewhorizon.gtnhlib.client.renderer.stacks.Vector3dStack;

/**
 * Standalone thread-local tessellator compatibility shell.
 */
public class LocalTessellator extends LegacyTessellator {

    boolean active = false;
    private final Vector3dStack storedTranslation = new Vector3dStack();

    public void storeTranslation() {
        storedTranslation.push();
        storedTranslation.set(xOffset, yOffset, zOffset);
    }

    public void restoreTranslation() {
        xOffset = storedTranslation.x;
        yOffset = storedTranslation.y;
        zOffset = storedTranslation.z;
        storedTranslation.pop();
    }
}
