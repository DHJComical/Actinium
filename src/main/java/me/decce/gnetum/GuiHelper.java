package me.decce.gnetum;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.launchwrapper.Launch;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class GuiHelper {
    private static final Gui gui;
    private static final MethodHandle Z_LEVEL_SETTER;
    private static boolean suppressErrorLog;

    public static void setZLevel(float zLevel) {
        try {
            Z_LEVEL_SETTER.invokeExact(gui, zLevel);
        } catch (Throwable e) {
            if (!suppressErrorLog) {
                suppressErrorLog = true;
                Gnetum.LOGGER.error("Failed to set zLevel!", e);
            }
        }
    }

    static {
        gui = Minecraft.getMinecraft().ingameGUI;
        Class<Gui> clazzGui = Gui.class;
        try {
            boolean deobfuscated = (boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
            Field field = clazzGui.getDeclaredField(deobfuscated ? "zLevel" : "field_73735_i");
            field.setAccessible(true);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Z_LEVEL_SETTER = lookup.unreflectSetter(field);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to initialize GuiHelper!", e);
        }
    }
}
