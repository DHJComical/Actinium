package me.decce.gnetum;

import me.decce.gnetum.mixins.early.MinecraftAccessor;
import net.minecraft.client.Minecraft;

public class FpsCounter {
    private int fps;
    private int frames;
    private long lastTime;

    public void reset() {
        this.lastTime = ((MinecraftAccessor)Minecraft.getMinecraft()).getDebugUpdateTime(); // makes sure the fps updates at the same time the game fps is updated
        this.frames = 0;
        this.fps = 0;
    }

    public void tick() {
        frames++;
        var currentTime = Minecraft.getSystemTime();
        if (currentTime >= lastTime + 2000L) {
            fps = 0;
            frames = 0;
            lastTime = Minecraft.getSystemTime();
        }
        else if (currentTime >= lastTime + 1000L) {
            fps = frames;
            frames = 0;
            lastTime = Minecraft.getSystemTime();
        }
    }

    public int getFps() {
        return fps;
    }
}
