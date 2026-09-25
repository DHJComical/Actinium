package me.flashyreese.mods.reeses_sodium_options.client.gui;

import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.Nullable;

/**
 * Exposes the screen to return to from whichever options screen is currently open (Reese's
 * Sodium Options' own screen or Sodium's, when RSO is disabled), so the screen can be reopened
 * and re-routed through the video settings mixin when the "enabled" option is toggled.
 */
public interface PreviousScreenHolder {
    @Nullable GuiScreen rso$previousScreen();
}
