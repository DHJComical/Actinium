package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.OptionGroup;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;

final class OptionPageBuilderImpl implements OptionPageBuilder {
    private ITextComponent name;
    private final List<OptionGroup> groups = new ArrayList<>();
    private final List<OptionBuilder> looseOptions = new ArrayList<>();

    OptionPage build() {
        List<OptionGroup> builtGroups = new ArrayList<>(this.groups);
        if (!this.looseOptions.isEmpty()) {
            OptionGroupBuilderImpl implicitGroup = new OptionGroupBuilderImpl();
            this.looseOptions.forEach(implicitGroup::addOption);
            builtGroups.add(implicitGroup.build());
        }
        return new OptionPage(this.name, builtGroups);
    }

    @Override
    public OptionPageBuilder setName(ITextComponent name) {
        this.name = name;
        return this;
    }

    @Override
    public OptionPageBuilder addOptionGroup(OptionGroupBuilder group) {
        if (!(group instanceof OptionGroupBuilderImpl implementation)) {
            throw new IllegalArgumentException("Option group builder was not created by this ConfigBuilder");
        }
        this.groups.add(implementation.build());
        return this;
    }

    @Override
    public OptionPageBuilder addOption(OptionBuilder option) {
        this.looseOptions.add(option);
        return this;
    }
}
