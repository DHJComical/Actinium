package com.dhj.actinium.gui.rso.compat;

/**
 * Stand-in for the newer Minecraft cursor types. The 1.12.2 client has no
 * cursor-switching API, so cursor requests are dropped at the extraction
 * layer; the constants are retained for source compatibility.
 */
public final class CursorTypes {
    public static final int POINTING_HAND = 0;
    public static final int RESIZE_EW = 1;
    public static final int TEXT = 2;

    private CursorTypes() {
    }
}
