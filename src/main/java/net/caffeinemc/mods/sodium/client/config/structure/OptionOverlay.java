package net.caffeinemc.mods.sodium.client.config.structure;

import net.minecraft.util.ResourceLocation;

import java.util.function.UnaryOperator;

/** Partial option change which is resolved against the current target after replacements. */
public record OptionOverlay(ResourceLocation target, String source, UnaryOperator<Option> change) {
}
