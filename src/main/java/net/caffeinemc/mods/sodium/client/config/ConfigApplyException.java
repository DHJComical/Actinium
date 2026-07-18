package net.caffeinemc.mods.sodium.client.config;

import net.minecraft.util.ResourceLocation;

/**
 * Identifies the exact option whose pending value prevented an atomic validation phase.
 */
public final class ConfigApplyException extends RuntimeException {
    private final ResourceLocation optionId;
    private final String phase;

    public ConfigApplyException(ResourceLocation optionId, Throwable cause) {
        this("validation", optionId, cause);
    }

    public ConfigApplyException(String phase, ResourceLocation optionId, Throwable cause) {
        super("Config apply failed during " + phase
                + (optionId == null ? "" : " for option '" + optionId + "'"), cause);
        this.phase = phase;
        this.optionId = optionId;
    }

    /** Returns the stable ID of the rejected option. */
    public ResourceLocation optionId() {
        return this.optionId;
    }

    /** Returns the deterministic transaction phase which failed. */
    public String phase() {
        return this.phase;
    }
}
