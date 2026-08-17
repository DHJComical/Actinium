package com.dhj.actinium.gui.rso.compat;

/**
 * Stand-in for the newer Minecraft render pipeline selector. The 1.12.2
 * client has a single GUI texture pipeline; the constants are retained so
 * Reese's Sodium Options call sites compile unchanged.
 */
public final class RenderPipelines {
    public static final int GUI = 0;
    public static final int GUI_TEXTURED = 1;
    public static final int GUI_TEXT_HIGHLIGHT = 2;

    private RenderPipelines() {
    }
}
