package com.dhj.actinium.gui.rso.compat;

/**
 * Newer-Minecraft style narration priority adapted to the 1.12.2 client.
 * Retained only for source compatibility; the 1.12.2 client has no screen
 * reader integration, so priorities carry no runtime effect.
 */
public enum NarrationPriority {
    NONE,
    HOVERED,
    FOCUSED
}
