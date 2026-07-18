package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.FlagHook;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

/** Immutable function-backed flag hook created by the builder convenience overload. */
public final class FlagHookImpl implements FlagHook {
    private final BiConsumer<Collection<ResourceLocation>, ConfigState> hook;
    private final List<ResourceLocation> triggers;

    public FlagHookImpl(BiConsumer<Collection<ResourceLocation>, ConfigState> hook,
                        Collection<ResourceLocation> triggers) {
        this.hook = hook;
        this.triggers = List.copyOf(triggers);
        if (this.triggers.isEmpty()) {
            throw new IllegalArgumentException("Flag hook must declare at least one trigger");
        }
    }

    @Override
    public Collection<ResourceLocation> getTriggers() {
        return this.triggers;
    }

    @Override
    public void accept(Collection<ResourceLocation> flags, ConfigState state) {
        this.hook.accept(flags, state);
    }
}
