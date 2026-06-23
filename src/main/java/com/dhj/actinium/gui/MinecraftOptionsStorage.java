package com.dhj.actinium.gui;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import org.embeddedt.embeddium.api.options.structure.OptionFlag;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import com.dhj.actinium.runtime.ActiniumRuntime;

public class MinecraftOptionsStorage implements OptionStorage<GameSettings> {
    private final Minecraft client;

    public MinecraftOptionsStorage() {
        this.client = Minecraft.getMinecraft();
    }

    @Override
    public GameSettings getData() {
        return this.client.gameSettings;
    }

    @Override
    public void save(Set<OptionFlag> flags) {
        this.getData().saveOptions();

        ActiniumRuntime.logger().info("Flushed changes to Minecraft configuration");
    }
}
