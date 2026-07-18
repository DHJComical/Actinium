package net.caffeinemc.mods.sodium.client.gui.input;

import net.caffeinemc.mods.sodium.client.gui.GuiRect;

/** Pure pointer priority rule for tooltip overlays rendered above option controls. */
public final class OverlayInputArbiter {
    private OverlayInputArbiter() {
    }

    public static boolean captures(boolean overlay, boolean visible, GuiRect bounds, int mouseX, int mouseY) {
        return overlay && visible && bounds != null && bounds.contains(mouseX, mouseY);
    }
}
