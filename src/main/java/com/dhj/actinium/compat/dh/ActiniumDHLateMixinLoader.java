package com.dhj.actinium.compat.dh;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ActiniumDHLateMixinLoader implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        List<String> mixins = new ArrayList<>();

        if (Loader.isModLoaded("distanthorizons")) {
            mixins.add("mixins.actinium.dh.json");
        }

        return mixins;
    }
}
