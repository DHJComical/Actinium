package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ColorThemeBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ExternalPageBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ExternalButtonOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Default builder implementation scoped to one explicitly registered entry point.
 */
public final class ConfigBuilderImpl implements ConfigBuilder {
    private final Function<String, ConfigManager.ModMetadata> metadataProvider;
    private final String defaultConfigId;
    private final List<ModOptionsBuilderImpl> modOptions = new ArrayList<>();

    public ConfigBuilderImpl(Function<String, ConfigManager.ModMetadata> metadataProvider, String defaultConfigId) {
        this.metadataProvider = metadataProvider;
        this.defaultConfigId = defaultConfigId;
    }

    /** Closes all pending builders and returns immutable model contributions. */
    public List<ModOptions> build() {
        Set<String> ids = new LinkedHashSet<>();
        List<ModOptions> built = new ArrayList<>();
        for (ModOptionsBuilderImpl builder : this.modOptions) {
            ModOptions options = builder.build();
            if (!ids.add(options.configId())) {
                throw new IllegalArgumentException("Duplicate config ID in entry point: " + options.configId());
            }
            built.add(options);
        }
        if (built.isEmpty()) {
            throw new IllegalStateException("Entry point '" + this.defaultConfigId + "' registered no config models");
        }
        return List.copyOf(built);
    }

    @Override
    public ModOptionsBuilder registerModOptions(String configId, String name, String version) {
        ModOptionsBuilderImpl builder = new ModOptionsBuilderImpl(configId, name, version);
        this.modOptions.add(builder);
        return builder;
    }

    @Override
    public ModOptionsBuilder registerModOptions(String configId) {
        ConfigManager.ModMetadata metadata = this.metadataProvider.apply(configId);
        if (metadata == null) {
            throw new IllegalArgumentException("No metadata registered for config '" + configId + "'");
        }
        return this.registerModOptions(configId, metadata.modName(), metadata.modVersion());
    }

    @Override
    public ModOptionsBuilder registerOwnModOptions() {
        return this.registerModOptions(this.defaultConfigId);
    }

    @Override
    public ColorThemeBuilder createColorTheme() {
        return new ColorThemeBuilderImpl();
    }

    @Override
    public OptionPageBuilder createOptionPage() {
        return new OptionPageBuilderImpl();
    }

    @Override
    public ExternalPageBuilder createExternalPage() {
        return new ExternalPageBuilderImpl();
    }

    @Override
    public OptionGroupBuilder createOptionGroup() {
        return new OptionGroupBuilderImpl();
    }

    @Override
    public BooleanOptionBuilder createBooleanOption(ResourceLocation id) {
        return new BooleanOptionBuilderImpl(id);
    }

    @Override
    public IntegerOptionBuilder createIntegerOption(ResourceLocation id) {
        return new IntegerOptionBuilderImpl(id);
    }

    @Override
    public <E extends Enum<E>> EnumOptionBuilder<E> createEnumOption(ResourceLocation id, Class<E> enumClass) {
        return new EnumOptionBuilderImpl<>(id, enumClass);
    }

    @Override
    public ExternalButtonOptionBuilder createExternalButtonOption(ResourceLocation id) {
        return new ExternalButtonOptionBuilderImpl(id);
    }
}
