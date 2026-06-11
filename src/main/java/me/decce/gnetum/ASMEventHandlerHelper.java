package me.decce.gnetum;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;

public class ASMEventHandlerHelper {
    private static final ReferenceSet<ASMEventHandler> invalid = new ReferenceOpenHashSet<>(0);
    private static final Reference2ObjectMap<ASMEventHandler, String> mapModId = new Reference2ObjectOpenHashMap<>();

    public static String tryGetModId(ASMEventHandler asm) {
        if (mapModId.containsKey(asm)) {
            return mapModId.get(asm);
        }
        if (invalid.contains(asm)) {
            return null;
        }
        Class<?> clazz = tryGetClass(asm);
        if (clazz == null) {
            invalid.add(asm);
            return null;
        }
        CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            invalid.add(asm);
            return null;
        }
        URL url = codeSource.getLocation();
        var mods = Loader.instance().getActiveModList();
        for (int i = 0; i < mods.size(); i++) {
            var mod = mods.get(i);
            File file = mod.getSource();
            if (url.getFile().contains(file.getAbsolutePath())) {
                String modid = mod.getModId();
                mapModId.put(asm, modid);
                return modid;
            }
        }
        invalid.add(asm);
        return null;
    }

    // Find out which class the given event handler will call
    private static Class<?> tryGetClass(ASMEventHandler asm) {
        String name = asm.toString().substring(5);
        if (name.startsWith("class ")) {
            name = name.substring(6);
            name = name.substring(0, name.indexOf(' '));
            try {
                Class<?> clazz = Class.forName(name);
                return clazz;
            } catch (Throwable ignored) {
                return null;
            }
        } else if (name.contains("@")) {
            name = name.substring(0, name.indexOf('@'));
            //Gnetum.LOGGER.info("name2="+name);
            try {
                Class<?> clazz = Class.forName(name);
                return clazz;
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }
}
