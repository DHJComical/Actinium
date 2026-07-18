package net.caffeinemc.mods.sodium.client.config.value;

import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.Set;

/**
 * Value which may derive from explicitly declared configuration dependencies.
 */
public interface DependentValue<V> {
    /** Evaluates the value against the current transaction state. */
    V get(Config state);

    /** Returns every option ID this value may read. */
    default Collection<ResourceLocation> getDependencies() {
        return Set.of();
    }
}
