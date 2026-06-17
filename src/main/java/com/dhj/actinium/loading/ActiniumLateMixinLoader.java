package com.dhj.actinium.loading;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class ActiniumLateMixinLoader implements ILateMixinLoader {
    private static final String GIBBED_MOD_ID = "gibbed";
    private static final String GIBBED_MIXIN_CONFIG = "mixins.actinium.mod.gibbed.json";

    @Override
    public List<String> getMixinConfigs() {
        if (Loader.isModLoaded(GIBBED_MOD_ID)) {
            return Collections.singletonList(GIBBED_MIXIN_CONFIG);
        }

        return Collections.emptyList();
    }
}
