package org.taumc.celeritas.impl.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import org.taumc.celeritas.api.options.structure.OptionFlag;
import org.taumc.celeritas.api.options.structure.OptionStorage;

import java.util.Set;

/**
 * Legacy storage facade for Minecraft's client settings.
 */
public final class MinecraftOptionsStorage implements OptionStorage<GameSettings> {
    private final Minecraft client = Minecraft.getMinecraft();

    @Override
    public GameSettings getData() {
        return client.gameSettings;
    }

    @Override
    public void save(Set<OptionFlag> flags) {
        getData().saveOptions();
    }
}
