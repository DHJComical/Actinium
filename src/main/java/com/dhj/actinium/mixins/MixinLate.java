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

    @Override
    public List<String> getMixinConfigs() {
        return configsFor(Loader::isModLoaded);
    }

    @Override
    public void onMixinConfigQueued(Context context) {
        if (!"mixins.actinium.dh.json".equals(context.mixinConfig())) {
            return;
        }

        MixinReEntranceLockFix.clearLeakedLock();
        MixinReEntranceLockFix.clearInvalidVanillaClasses();
        try {
            MixinReEntranceLockFix.preloadClasses(EntityRenderer.class);
        } finally {
            MixinReEntranceLockFix.clearLeakedLock();
            MixinReEntranceLockFix.clearInvalidVanillaClasses();
        }
    }

    static List<String> configsFor(Predicate<String> loadedMods) {
        List<String> mixins = new ArrayList<>();

        if (loadedMods.test("distanthorizons")) {
            mixins.add("mixins.actinium.dh.json");
        }

        if (loadedMods.test("gibbed")) {
            mixins.add("mixins.actinium.gibbed.json");
        }

        if (loadedMods.test("ichunutil")) {
            mixins.add("mixins.actinium.ichunutil.json");
        }

        if (loadedMods.test("lumenized")) {
            mixins.add("mixins.actinium.lumenized.json");
        }

        if (loadedMods.test("neofontrender_ui_enhancements")) {
            mixins.add("mixins.actinium.revoui.json");
        }

        if (loadedMods.test("betterfoliage")) {
            mixins.add("mixins.actinium.betterfoliage.json");
        }

        if (loadedMods.test("codechickenlib")) {
            mixins.add("mixins.actinium.ccl.json");
        }

        return mixins;
    }
}
