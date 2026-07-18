package net.caffeinemc.mods.sodium.api.config;

import net.minecraft.util.ResourceLocation;

/**
 * Read-only configuration view exposed to declared dependency providers.
 */
public interface ConfigState {
    /** Re-evaluates a provider whenever the screen model is rebuilt. */
    ResourceLocation UPDATE_ON_REBUILD = new ResourceLocation("__meta__", "update_on_rebuild");
    /** Allows a provider to observe its parent option after that option is applied. */
    ResourceLocation UPDATE_ON_APPLY = new ResourceLocation("__meta__", "update_on_apply");

    /** Reads a boolean option by stable ID. */
    boolean readBooleanOption(ResourceLocation id);

    /** Reads an integer option by stable ID. */
    int readIntOption(ResourceLocation id);

    /** Reads an enum option by stable ID and verifies its declared enum type. */
    <E extends Enum<E>> E readEnumOption(ResourceLocation id, Class<E> enumClass);
}
