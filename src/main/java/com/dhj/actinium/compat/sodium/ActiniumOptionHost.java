package com.dhj.actinium.compat.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.option.FmlRsoModMetadataResolver;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoModMetadataResolver;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoModOptions;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionFlag;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Actinium's own option host: collects all embeddium {@link OptionPage}s
 * (built-in Actinium pages, the RSO config page, Iris pages and third-party
 * event contributions), groups them by modId into {@link RsoModOptions}
 * views, and coordinates apply/undo and flag side effects. No Sodium config
 * model is involved.
 */
public final class ActiniumOptionHost {
    private static final Logger LOGGER = LogManager.getLogger("Actinium-OptionHost");
    private static final class SharedHolder {
        private static final ActiniumOptionHost INSTANCE = new ActiniumOptionHost(
                new ActiniumApplyActionsImpl(Minecraft.getMinecraft()),
                FmlRsoModMetadataResolver.forClient(Minecraft.getMinecraft().getResourceManager()));
    }

    private final ActiniumApplyActions applyActions;
    private final RsoModMetadataResolver metadataResolver;
    private List<RsoModOptions> modOptions;

    private ActiniumOptionHost(ActiniumApplyActions applyActions, RsoModMetadataResolver metadataResolver) {
        this.applyActions = applyActions;
        this.metadataResolver = metadataResolver;
    }

    /** Returns the process-wide host (pages are collected and frozen on first access). */
    public static ActiniumOptionHost shared() {
        return SharedHolder.INSTANCE;
    }

    /** Test entry point for injecting action and metadata boundaries. */
    public static ActiniumOptionHost create(ActiniumApplyActions applyActions, RsoModMetadataResolver metadataResolver) {
        return new ActiniumOptionHost(applyActions, metadataResolver);
    }

    /** Collects and groups all option pages (lazy, one-time). */
    public synchronized List<RsoModOptions> modOptions() {
        if (this.modOptions == null) {
            this.modOptions = this.collect();
        }
        return this.modOptions;
    }

    private List<RsoModOptions> collect() {
        List<OptionPage> pages = new ArrayList<>();
        pages.addAll(ActiniumOptionPages.builtInPages());
        pages.add(me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfigEntryPoint.createOptionsPage());
        OptionGUIConstructionBridge.collectExtensions(pages);
        Map<String, List<OptionPage>> byMod = new LinkedHashMap<>();
        for (OptionPage page : pages) {
            String modId = page.getId().getModId();
            if (modId == null || modId.isBlank()) {
                throw new IllegalArgumentException("Option page has a blank namespace: " + page.getId());
            }
            byMod.computeIfAbsent(modId, ignored -> new ArrayList<>()).add(page);
        }
        List<RsoModOptions> result = new ArrayList<>();
        for (Map.Entry<String, List<OptionPage>> entry : byMod.entrySet()) {
            result.add(RsoModOptions.create(entry.getKey(),
                    this.metadataResolver.resolve(entry.getKey()), entry.getValue()));
        }
        LOGGER.info("Collected {} option page groups: {}", result.size(),
                result.stream().map(RsoModOptions::configId).toList());
        return List.copyOf(result);
    }

    /** Applies all pending changes and triggers de-duplicated flag side effects. */
    public void applyChanges() {
        Set<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);
        Set<OptionStorage<?>> dirtyStorages = new java.util.HashSet<>();
        for (OptionPage page : this.allPages()) {
            for (Option<?> option : page.getOptions()) {
                if (option.hasChanged()) {
                    option.applyChanges();
                    flags.addAll(option.getFlags());
                    dirtyStorages.add(option.getStorage());
                }
            }
        }
        for (OptionStorage<?> storage : dirtyStorages) {
            storage.save(flags);
        }
        this.applyFlagSideEffects(flags);
    }

    /** Discards all pending changes (back to the applied baseline). */
    public void undoChanges() {
        for (OptionPage page : this.allPages()) {
            for (Option<?> option : page.getOptions()) {
                if (option.hasChanged()) {
                    option.reset();
                }
            }
        }
    }

    /** Returns whether any pending changes exist. */
    public boolean hasPendingChanges() {
        for (OptionPage page : this.allPages()) {
            for (Option<?> option : page.getOptions()) {
                if (option.hasChanged()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Restores every option to its declared default (pending state). */
    public void resetToDefaults() {
        for (OptionPage page : this.allPages()) {
            for (Option<?> option : page.getOptions()) {
                option.resetToDefault();
            }
        }
    }

    private List<OptionPage> allPages() {
        List<OptionPage> pages = new ArrayList<>();
        for (RsoModOptions options : this.modOptions()) {
            options.unwrapPages(pages);
        }
        return pages;
    }

    private void applyFlagSideEffects(Set<OptionFlag> flags) {
        if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD)) {
            this.applyActions.reloadRenderer();
        } else if (flags.contains(OptionFlag.REQUIRES_RENDERER_UPDATE)) {
            this.applyActions.updateRenderer();
        }
        if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD)) {
            this.applyActions.reloadAssets();
        }
        if (flags.contains(OptionFlag.REQUIRES_GAME_RESTART)) {
            this.applyActions.showRestartRequired();
        }
    }
}
