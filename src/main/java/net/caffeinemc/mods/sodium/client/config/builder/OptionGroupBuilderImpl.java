package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.OptionGroup;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;

final class OptionGroupBuilderImpl implements OptionGroupBuilder {
    private ITextComponent name;
    private final List<Option> options = new ArrayList<>();

    OptionGroup build() {
        return new OptionGroup(this.name, this.options);
    }

    @Override
    public OptionGroupBuilder setName(ITextComponent name) {
        this.name = name;
        return this;
    }

    @Override
    public OptionGroupBuilder addOption(OptionBuilder option) {
        if (!(option instanceof OptionBuilderImpl<?> implementation)) {
            throw new IllegalArgumentException("Option builder was not created by this ConfigBuilder");
        }
        this.options.add(implementation.build());
        return this;
    }
}
