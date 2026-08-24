package com.dhj.actinium.compat.sodium;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.text.TextComponentTranslation;

/** Performs Config flag actions against the active Minecraft 1.12.2 client. */
public final class ActiniumApplyActionsImpl implements ActiniumApplyActions {
    private final Minecraft client;

    /** Uses the supplied client to keep all side effects at the GUI integration boundary. */
    public ActiniumApplyActionsImpl(Minecraft client) {
        if (client == null) {
            throw new IllegalArgumentException("Minecraft client must not be null");
        }
        this.client = client;
    }

    @Override
    public void reloadRenderer() {
        if (this.client.world != null) {
            this.client.renderGlobal.loadRenderers();
        }
    }

    @Override
    public void updateRenderer() {
        if (this.client.world != null) {
            this.client.renderGlobal.setDisplayListEntitiesDirty();
        }
    }

    @Override
    public void reloadAssets() {
        this.client.getTextureMapBlocks().setMipmapLevels(this.client.gameSettings.mipmapLevels);
        // Keep the atlas filter state in sync with the new mip level count, matching vanilla
        // GameSettings#setOptionFloatValue for MIPMAP_LEVELS. Without this the texture keeps the
        // previous mipmap filter while the rebuilt atlas has a different number of mip levels.
        this.client.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        this.client.getTextureMapBlocks().setBlurMipmapDirect(false, this.client.gameSettings.mipmapLevels > 0);
        this.client.refreshResources();
    }

    @Override
    public void showRestartRequired() {
        if (this.client.ingameGUI != null) {
            this.client.ingameGUI.setOverlayMessage(new TextComponentTranslation("sodium.console.game_restart"), false);
        }
    }
}
