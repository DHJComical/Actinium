package net.caffeinemc.mods.sodium.client.gui.resource;

import net.minecraft.util.ResourceLocation;

/** Explicit resource existence boundary injectable for GUI initialization tests. */
@FunctionalInterface
public interface ResourceValidator {
    void require(ResourceLocation resource);
}
