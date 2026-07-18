package net.caffeinemc.mods.sodium.api.config.option;

import net.minecraft.util.ResourceLocation;

import java.util.Locale;

/**
 * Built-in actions which a GUI adapter may execute after an apply transaction.
 */
public enum OptionFlag {
    REQUIRES_RENDERER_RELOAD,
    REQUIRES_RENDERER_UPDATE,
    REQUIRES_ASSET_RELOAD,
    REQUIRES_VIDEOMODE_RELOAD,
    REQUIRES_GAME_RESTART;

    private final ResourceLocation id = new ResourceLocation(
            "sodium", "builtin_option_flag." + this.name().toLowerCase(Locale.ROOT));

    /** Returns the stable ID used by custom and built-in flag hooks. */
    public ResourceLocation getId() {
        return this.id;
    }
}
