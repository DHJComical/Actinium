package com.dhj.actinium.gui;

import com.dhj.actinium.compat.sodium.ActiniumOptionHost;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Set;

/**
 * Mod-list Config button factory: opens the RSO video options screen with
 * the mod list as its parent screen. Declared in mcmod.info under
 * "guiFactory".
 */
public final class ActiniumGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {
    }

    @Override
    public Set<IModGuiFactory.RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new SodiumVideoOptionsScreen(parentScreen, ActiniumOptionHost.shared().modOptions());
    }
}
