package me.flashyreese.mods.reeses_sodium_options.client.gui.option;

/**
 * Resolves the metadata a modId shows in the RSO tab header. The client
 * default implementation queries FML mod metadata and falls back to a table
 * for embedded components (Iris, RSO itself); tests inject fake resolvers.
 */
public interface RsoModMetadataResolver {
    /** Returns the display metadata for a configId; unknown ids fall back to the id itself. */
    RsoModMetadata resolve(String configId);
}
