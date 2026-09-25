package com.dhj.actinium.gui.rso.compat;

import net.minecraft.util.text.ITextComponent;

/**
 * Newer-Minecraft style formatted text adapted to the 1.12.2 client.
 * Anything that can render as a text string (component or raw text).
 */
public interface FormattedText {

    /** Returns the plain (unformatted) text content. */
    String getString();

    /** Unwraps to a 1.12.2 text component when available, or null. */
    default ITextComponent unwrap() {
        return null;
    }
}
