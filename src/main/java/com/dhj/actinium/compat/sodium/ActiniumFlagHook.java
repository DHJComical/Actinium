package com.dhj.actinium.compat.sodium;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.FlagHook;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.List;

/** Maps a transaction's deduplicated built-in flags to the retained Actinium client actions. */
public final class ActiniumFlagHook implements FlagHook {
    private static final List<ResourceLocation> TRIGGERS = List.of(
            OptionFlag.REQUIRES_RENDERER_RELOAD.getId(),
            OptionFlag.REQUIRES_RENDERER_UPDATE.getId(),
            OptionFlag.REQUIRES_ASSET_RELOAD.getId(),
            OptionFlag.REQUIRES_GAME_RESTART.getId());

    private final ActiniumApplyActions actions;

    /** Creates a hook whose action boundary can be replaced by logic tests. */
    public ActiniumFlagHook(ActiniumApplyActions actions) {
        if (actions == null) {
            throw new IllegalArgumentException("Apply actions must not be null");
        }
        this.actions = actions;
    }

    @Override
    public Collection<ResourceLocation> getTriggers() {
        return TRIGGERS;
    }

    @Override
    public void accept(Collection<ResourceLocation> flags, ConfigState state) {
        if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD.getId())) {
            this.actions.reloadRenderer();
        } else if (flags.contains(OptionFlag.REQUIRES_RENDERER_UPDATE.getId())) {
            this.actions.updateRenderer();
        }
        if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD.getId())) {
            this.actions.reloadAssets();
        }
        if (flags.contains(OptionFlag.REQUIRES_GAME_RESTART.getId())) {
            this.actions.showRestartRequired();
        }
    }
}
