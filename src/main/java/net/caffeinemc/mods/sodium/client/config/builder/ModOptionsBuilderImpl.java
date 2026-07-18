package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.FlagHook;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.PageBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ColorThemeBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;
import net.caffeinemc.mods.sodium.client.config.structure.FlagHookImpl;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.caffeinemc.mods.sodium.client.config.structure.OptionOverlay;
import net.caffeinemc.mods.sodium.client.config.structure.OptionOverride;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class ModOptionsBuilderImpl implements ModOptionsBuilder {
    private final String configId;
    private String name;
    private String version;
    private ColorTheme theme;
    private ResourceLocation icon;
    private boolean iconMonochrome = true;
    private final List<Page> pages = new ArrayList<>();
    private final List<OptionOverride> overrides = new ArrayList<>();
    private final List<OptionOverlay> overlays = new ArrayList<>();
    private final List<FlagHook> flagHooks = new ArrayList<>();

    ModOptionsBuilderImpl(String configId, String name, String version) {
        this.configId = configId;
        this.name = name;
        this.version = version;
    }

    ModOptions build() {
        ColorTheme resolvedTheme = this.theme == null ? ColorTheme.defaultFor(this.configId) : this.theme;
        return new ModOptions(this.configId, this.name, this.version, resolvedTheme, this.icon,
                this.iconMonochrome, this.pages, this.overrides, this.overlays, this.flagHooks);
    }

    @Override
    public ModOptionsBuilder setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public ModOptionsBuilder setVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public ModOptionsBuilder formatVersion(Function<String, String> formatter) {
        this.version = formatter.apply(this.version);
        return this;
    }

    @Override
    public ModOptionsBuilder setColorTheme(ColorThemeBuilder theme) {
        if (!(theme instanceof ColorThemeBuilderImpl implementation)) {
            throw new IllegalArgumentException("Color theme builder was not created by this ConfigBuilder");
        }
        this.theme = implementation.build();
        return this;
    }

    @Override
    public ModOptionsBuilder setIcon(ResourceLocation texture) {
        this.icon = texture;
        this.iconMonochrome = true;
        return this;
    }

    @Override
    public ModOptionsBuilder setNonTintedIcon(ResourceLocation texture) {
        this.icon = texture;
        this.iconMonochrome = false;
        return this;
    }

    @Override
    public ModOptionsBuilder addPage(PageBuilder page) {
        if (page instanceof OptionPageBuilderImpl implementation) {
            this.pages.add(implementation.build());
        } else if (page instanceof ExternalPageBuilderImpl implementation) {
            this.pages.add(implementation.build());
        } else {
            throw new IllegalArgumentException("Page builder was not created by this ConfigBuilder");
        }
        return this;
    }

    @Override
    public ModOptionsBuilder registerOptionReplacement(ResourceLocation target, OptionBuilder replacement) {
        if (!(replacement instanceof OptionBuilderImpl<?> implementation)) {
            throw new IllegalArgumentException("Replacement builder was not created by this ConfigBuilder");
        }
        this.overrides.add(new OptionOverride(target, this.configId, implementation.build()));
        return this;
    }

    @Override
    public ModOptionsBuilder registerOptionOverlay(ResourceLocation target, OptionBuilder overlay) {
        if (!(overlay instanceof OptionBuilderImpl<?> implementation)) {
            throw new IllegalArgumentException("Overlay builder was not created by this ConfigBuilder");
        }
        this.overlays.add(new OptionOverlay(target, this.configId, implementation::buildWithBaseOption));
        return this;
    }

    @Override
    public ModOptionsBuilder registerFlagHook(BiConsumer<Collection<ResourceLocation>, ConfigState> hook,
                                              ResourceLocation... triggers) {
        return this.registerFlagHook(new FlagHookImpl(hook, Arrays.asList(triggers)));
    }

    @Override
    public ModOptionsBuilder registerFlagHook(FlagHook hook) {
        if (hook == null) {
            throw new IllegalArgumentException("Flag hook must not be null for config '" + this.configId + "'");
        }
        this.flagHooks.add(hook);
        return this;
    }
}
