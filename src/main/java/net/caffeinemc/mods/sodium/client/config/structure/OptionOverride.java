package net.caffeinemc.mods.sodium.client.config.structure;

import net.minecraft.util.ResourceLocation;

/** Complete option replacement contributed by another configuration owner. */
public record OptionOverride(ResourceLocation target, String source, Option change) {
}
