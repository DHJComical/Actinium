package me.decce.gnetum;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.util.ResourceLocation;

public class Icons {
    public static final ResourceLocation TICK;
    public static final ResourceLocation EXCLAMATION;
    public static final ResourceLocation QUESTION;

    static {
        TICK = new ResourceLocation("gnetum", "icons/tick.png");
        EXCLAMATION = new ResourceLocation("gnetum", "icons/exclamation.png");
        QUESTION = new ResourceLocation("gnetum", "icons/question.png");
        Minecraft.getMinecraft().getTextureManager().loadTexture(TICK, new SimpleTexture(TICK));
        Minecraft.getMinecraft().getTextureManager().loadTexture(EXCLAMATION, new SimpleTexture(EXCLAMATION));
        Minecraft.getMinecraft().getTextureManager().loadTexture(QUESTION, new SimpleTexture(QUESTION));
    }
}
