package com.dhj.actinium.compat.sodium;

import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiniumFlagHookTest {
    @Test
    void deduplicatesActionsAndPrefersRendererReloadOverUpdate() {
        RecordingActions actions = new RecordingActions();
        ActiniumFlagHook hook = new ActiniumFlagHook(actions);
        LinkedHashSet<ResourceLocation> flags = new LinkedHashSet<>();
        flags.add(OptionFlag.REQUIRES_RENDERER_UPDATE.getId());
        flags.add(OptionFlag.REQUIRES_RENDERER_RELOAD.getId());
        flags.add(OptionFlag.REQUIRES_ASSET_RELOAD.getId());
        flags.add(OptionFlag.REQUIRES_GAME_RESTART.getId());

        hook.accept(flags, null);

        assertEquals(1, actions.rendererReloads);
        assertEquals(0, actions.rendererUpdates);
        assertEquals(1, actions.assetReloads);
        assertEquals(1, actions.restartMessages);
    }

    private static final class RecordingActions implements ActiniumApplyActions {
        private int rendererReloads;
        private int rendererUpdates;
        private int assetReloads;
        private int restartMessages;

        @Override
        public void reloadRenderer() {
            this.rendererReloads++;
        }

        @Override
        public void updateRenderer() {
            this.rendererUpdates++;
        }

        @Override
        public void reloadAssets() {
            this.assetReloads++;
        }

        @Override
        public void showRestartRequired() {
            this.restartMessages++;
        }
    }
}
