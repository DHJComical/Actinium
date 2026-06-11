package me.decce.gnetum.compat.valkyrie;

import dev.redstudio.valkyrie.config.ValkyrieConfig;
import net.minecraftforge.fml.common.Loader;

public class ValkyrieCompat {
    public static boolean modInstalled = Loader.isModLoaded("valkyrie");

    public static int getOffset() {
        return ValkyrieConfig.mc67532Fix.enabled ? ValkyrieConfig.mc67532Fix.offset : 0;
    }
}
