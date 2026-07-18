package net.caffeinemc.mods.sodium.api.config.option;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * Receives the deduplicated flags produced by an apply transaction.
 */
public interface FlagHook extends BiConsumer<Collection<ResourceLocation>, ConfigState> {
    /** Returns the flag IDs which activate this hook. */
    Collection<ResourceLocation> getTriggers();
}
