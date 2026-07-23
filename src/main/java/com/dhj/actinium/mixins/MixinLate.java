package com.dhj.actinium.mixins;

import com.dhj.actinium.compat.MixinReEntranceLockFix;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.Context;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class MixinLate implements ILateMixinLoader {
    private static final String DISTANT_HORIZONS_MOD_ID = "distanthorizons";
    private static final String DISTANT_HORIZONS_MIXIN_CONFIG = "mixins.actinium.dh.json";
    private static final String GIBBED_MOD_ID = "gibbed";
    private static final String GIBBED_MIXIN_CONFIG = "mixins.actinium.gibbed.json";
    private static final String LUMENIZED_MOD_ID = "lumenized";
    private static final String LUMENIZED_MIXIN_CONFIG = "mixins.actinium.lumenized.json";

    @Override
    public List<String> getMixinConfigs() {
        return configsFor(Loader::isModLoaded);
    }

    @Override
    public void onMixinConfigQueued(Context context) {
        if (!DISTANT_HORIZONS_MIXIN_CONFIG.equals(context.mixinConfig())) {
            return;
        }

        MixinReEntranceLockFix.clearLeakedLock();
        MixinReEntranceLockFix.clearInvalidVanillaClasses();
        MixinReEntranceLockFix.preloadClasses(EntityRenderer.class);
        MixinReEntranceLockFix.clearLeakedLock();
        MixinReEntranceLockFix.clearInvalidVanillaClasses();
    }

    static List<String> configsFor(Predicate<String> loadedMods) {
        List<String> mixins = new ArrayList<>();

        if (loadedMods.test(DISTANT_HORIZONS_MOD_ID)) {
            mixins.add(DISTANT_HORIZONS_MIXIN_CONFIG);
        }

        if (loadedMods.test(GIBBED_MOD_ID)) {
            mixins.add(GIBBED_MIXIN_CONFIG);
        }

        if (loadedMods.test(LUMENIZED_MOD_ID)) {
            mixins.add(LUMENIZED_MIXIN_CONFIG);
        }

        return mixins;
    }
}
