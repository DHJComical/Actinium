package net.caffeinemc.mods.sodium.client.config.value;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Dynamic value whose read access is constrained to its declared dependency IDs.
 */
public final class DynamicValue<V> implements DependentValue<V>, ConfigState {
    private final Function<ConfigState, V> provider;
    private final Set<ResourceLocation> dependencies;
    private Config state;
    private ResourceLocation parentOption;

    public DynamicValue(Function<ConfigState, V> provider, ResourceLocation[] dependencies) {
        this.provider = provider;
        this.dependencies = Set.copyOf(new LinkedHashSet<>(Arrays.asList(dependencies)));
    }

    @Override
    public V get(Config state) {
        if (this.state != null) {
            throw new IllegalStateException("Dynamic configuration value evaluation is recursive");
        }
        this.state = state;
        try {
            V value = this.provider.apply(this);
            if (value == null) {
                throw new IllegalStateException("Dynamic configuration value provider returned null");
            }
            return value;
        } finally {
            this.state = null;
        }
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return this.dependencies;
    }

    /** Authorizes UPDATE_ON_APPLY providers to read their owning option's applied value. */
    public void authorizeParentOption(ResourceLocation parentOption) {
        this.parentOption = parentOption;
    }

    @Override
    public boolean readBooleanOption(ResourceLocation id) {
        return this.isAppliedParentRead(id)
                ? this.requireState().readAppliedBooleanOption(id)
                : this.requireState().readBooleanOption(id);
    }

    @Override
    public int readIntOption(ResourceLocation id) {
        return this.isAppliedParentRead(id)
                ? this.requireState().readAppliedIntOption(id)
                : this.requireState().readIntOption(id);
    }

    @Override
    public <E extends Enum<E>> E readEnumOption(ResourceLocation id, Class<E> enumClass) {
        return this.isAppliedParentRead(id)
                ? this.requireState().readAppliedEnumOption(id, enumClass)
                : this.requireState().readEnumOption(id, enumClass);
    }

    private boolean isAppliedParentRead(ResourceLocation id) {
        if (this.dependencies.contains(id)) {
            return false;
        }
        if (this.dependencies.contains(ConfigState.UPDATE_ON_APPLY) && id.equals(this.parentOption)) {
            return true;
        }
        throw new IllegalStateException("Attempted to read undeclared configuration dependency: " + id);
    }

    private Config requireState() {
        if (this.state == null) {
            throw new IllegalStateException("Dynamic configuration value was read outside evaluation");
        }
        return this.state;
    }
}
