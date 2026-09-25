package me.flashyreese.mods.reeses_sodium_options.client.gui.option;

/**
 * Narrow view of FML mod metadata: the display name and version needed by
 * the RSO tab header. Extracted from a {@code ModContainer} by the client
 * adapter; tests inject fake implementations.
 */
public interface RsoModInfo {
    String displayName();

    String version();
}
