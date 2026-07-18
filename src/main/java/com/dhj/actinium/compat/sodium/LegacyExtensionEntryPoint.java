package com.dhj.actinium.compat.sodium;

import com.dhj.actinium.runtime.ActiniumRuntime;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.embeddium.api.options.structure.OptionPage;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/** Converts all legacy pages contributed by one auditable third-party namespace. */
public final class LegacyExtensionEntryPoint implements ConfigEntryPoint {
    private final String namespace;
    private final List<OptionPage> pages;
    private final Set<ResourceLocation> registeredOptionIds = new LinkedHashSet<>();

    /** Groups pages by namespace so one broken third party cannot invalidate another party's pages. */
    public LegacyExtensionEntryPoint(String namespace, List<OptionPage> pages) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("Legacy extension namespace must not be blank");
        }
        this.namespace = namespace;
        this.pages = List.copyOf(pages);
    }

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        ModOptionsBuilder owner = builder.registerModOptions(this.namespace, this.namespace, "legacy-event");
        new LegacyOptionAdapter(builder, !ActiniumRuntime.options().isReadOnly(),
                this.registeredOptionIds).addPages(owner, this.pages);
    }
}
