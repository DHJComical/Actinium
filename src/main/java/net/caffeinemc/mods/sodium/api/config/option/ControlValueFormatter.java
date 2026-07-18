package net.caffeinemc.mods.sodium.api.config.option;

import net.minecraft.util.text.ITextComponent;

/**
 * Formats numeric option values without coupling the model to a concrete control.
 */
@FunctionalInterface
public interface ControlValueFormatter {
    /** Produces the display text for a numeric value. */
    ITextComponent format(int value);
}
