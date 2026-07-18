package com.dhj.actinium.compat.sodium;

import com.dhj.actinium.gui.ActiniumGameOptionPages;
import com.dhj.actinium.runtime.ActiniumRuntime;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.option.FlagHook;
import net.caffeinemc.mods.sodium.api.config.structure.ColorThemeBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.impl.gui.options.CommonOptionPages;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/** Registers every retained Actinium video option against the modern Config API. */
public final class ActiniumConfigEntryPoint implements ConfigEntryPoint {
    private static final ResourceLocation ICON =
            new ResourceLocation(ActiniumRuntime.MODID, "icon.png");
    private final List<OptionPage> pages;
    private final FlagHook flagHook;
    private final Set<ResourceLocation> registeredOptionIds = new LinkedHashSet<>();

    /** Creates the required core entrypoint from an already constructed legacy page snapshot. */
    public ActiniumConfigEntryPoint(List<OptionPage> pages, FlagHook flagHook) {
        this.pages = List.copyOf(pages);
        this.flagHook = flagHook;
    }

    /** Builds the five pages currently exposed by Actinium's retained video settings screen. */
    public static List<OptionPage> createLegacyPages() {
        return List.of(
                ActiniumGameOptionPages.general(),
                ActiniumGameOptionPages.quality(),
                CommonOptionPages.performance(ActiniumRuntime.options()),
                ActiniumGameOptionPages.advanced(),
                ActiniumGameOptionPages.debug());
    }

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        ColorThemeBuilder theme = builder.createColorTheme().setFullThemeRGB(0x9B59B6, 0xC39BD3, 0x6C3483);
        ModOptionsBuilder owner = builder.registerModOptions(
                        ActiniumRuntime.MODID, "Actinium", ActiniumRuntime.version())
                .setColorTheme(theme)
                .setNonTintedIcon(ICON)
                .registerFlagHook(this.flagHook);
        new LegacyOptionAdapter(builder, !ActiniumRuntime.options().isReadOnly(), this.registeredOptionIds)
                .addPages(owner, this.pages);
    }
}
