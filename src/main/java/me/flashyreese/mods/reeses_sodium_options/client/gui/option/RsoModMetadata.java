package me.flashyreese.mods.reeses_sodium_options.client.gui.option;

import net.minecraft.util.ResourceLocation;

/**
 * Mod metadata shown in the RSO tab header: display name, version and an
 * optional icon. Resolved by {@link RsoModMetadataResolver} (FML mod
 * metadata or the embedded-component fallback).
 */
public record RsoModMetadata(String name, String version, ResourceLocation icon, boolean iconMonochrome) {
    public RsoModMetadata {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Mod display name must not be blank");
        }
        if (version == null) {
            throw new IllegalArgumentException("Mod version must not be null");
        }
    }
}
