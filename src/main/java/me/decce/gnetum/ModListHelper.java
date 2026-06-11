package me.decce.gnetum;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.util.Map;

public class ModListHelper {
    private static final Map<String, ModContainer> map = Loader.instance().getIndexedModList();

    public static String getModName(String modid) {
        var mod = map.get(modid);
        if (mod == null) {
            return null;
        }
        return mod.getName();
    }
}
