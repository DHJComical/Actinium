package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.client.config.search.OptionTextSource;
import net.caffeinemc.mods.sodium.client.config.search.SearchIndex;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.caffeinemc.mods.sodium.client.config.value.DynamicValue;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Collection;
import java.util.Set;

/**
 * Base immutable option definition with transaction state owned by stateful subclasses.
 */
public abstract class Option {
    private final ResourceLocation id;
    private final Set<ResourceLocation> dependencies;
    private final ITextComponent name;
    private final DependentValue<Boolean> enabled;
    protected Config state;

    protected Option(ResourceLocation id, Collection<ResourceLocation> dependencies, ITextComponent name,
                     DependentValue<Boolean> enabled) {
        if (id == null) {
            throw new IllegalArgumentException("Option ID must not be null");
        }
        if (dependencies.contains(id)) {
            throw new IllegalArgumentException("Option '" + id + "' cannot depend on itself");
        }
        if (name == null || name.getUnformattedText().isBlank()) {
            throw new IllegalArgumentException("Option '" + id + "' name must not be blank");
        }
        this.id = id;
        this.dependencies = Set.copyOf(dependencies);
        this.name = name;
        this.enabled = enabled;
        authorizeParent(enabled, id);
    }

    /** Returns the stable globally unique option ID. */
    public final ResourceLocation getId() {
        return this.id;
    }

    /** Returns all IDs required by dynamic option properties. */
    public final Set<ResourceLocation> getDependencies() {
        return this.dependencies;
    }

    /** Returns the localized option name. */
    public final ITextComponent getName() {
        return this.name;
    }

    /** Returns whether dependency conditions currently permit interaction. */
    public final boolean isEnabled() {
        return this.enabled.get(this.requireState());
    }

    /** Returns the enabled-state provider for dependency-aware GUI inspection. */
    public final DependentValue<Boolean> getEnabled() {
        return this.enabled;
    }

    /** Returns the localized, value-sensitive description. */
    public abstract ITextComponent getTooltip();

    /** Returns the optional impact label. */
    public OptionImpact getImpact() {
        return null;
    }

    /** Returns apply action IDs. */
    public Set<ResourceLocation> getFlags() {
        return Set.of();
    }

    /** Returns whether this option owns unapplied state. */
    public boolean hasChanged() {
        return false;
    }

    /** Moves pending state to the current applied baseline without persistence. */
    public void undo() {
    }

    /** Moves pending state to the declared default without persistence. */
    public void resetToDefault() {
    }

    /** Registers page, option name, and description sources for this option. */
    public void registerTextSources(SearchIndex index, ModOptions modOptions, OptionPage page,
                                    OptionGroup optionGroup) {
        index.register(new OptionTextSource(this, modOptions, page, optionGroup,
                OptionTextSource.Kind.PAGE, () -> page.name().getUnformattedText()));
        index.register(new OptionTextSource(this, modOptions, page, optionGroup,
                OptionTextSource.Kind.NAME, () -> this.getName().getUnformattedText()));
        index.register(new OptionTextSource(this, modOptions, page, optionGroup,
                OptionTextSource.Kind.DESCRIPTION, () -> this.getTooltip().getUnformattedText()));
    }

    final void attach(Config state) {
        if (this.state != null) {
            throw new IllegalStateException("Option '" + this.id + "' is already attached to a config");
        }
        this.state = state;
    }

    final Config requireState() {
        if (this.state == null) {
            throw new IllegalStateException("Option '" + this.id + "' is not attached to a config");
        }
        return this.state;
    }

    static void authorizeParent(DependentValue<?> value, ResourceLocation id) {
        if (value instanceof DynamicValue<?> dynamicValue) {
            dynamicValue.authorizeParentOption(id);
        }
    }
}
