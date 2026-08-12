package com.dhj.actinium.mixins;

import com.dhj.actinium.compat.MixinReEntranceLockFix;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.Context;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class MixinLate implements ILateMixinLoader {

    /** Late/conditional configs. Each config declares its required mod ids in its "mods" field. */
    private static final List<String> CONDITIONAL_CONFIGS = List.of(
        "mixins.actinium.dh.json",
        "mixins.actinium.gibbed.json",
        "mixins.actinium.ichunutil.json",
        "mixins.actinium.lumenized.json",
        "mixins.actinium.revoui.json",
        "mixins.actinium.betterfoliage.json",
        "mixins.actinium.ccl.json"
    );

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

    /** Returns the conditional configs whose declared mod ids are all loaded. */
    static List<String> configsFor(Predicate<String> loadedMods) {
        List<String> mixins = new ArrayList<>();
        for (String config : CONDITIONAL_CONFIGS) {
            if (modsLoaded(config, loadedMods)) {
                mixins.add(config);
            }
        }
        return mixins;
    }

    private static boolean modsLoaded(String configName, Predicate<String> loadedMods) {
        try (InputStream stream = MixinLate.class.getClassLoader().getResourceAsStream(configName)) {
            if (stream == null) {
                throw new IllegalStateException("Missing mixin config: " + configName);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                final JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
                final JsonArray mods = config.getAsJsonArray("mods");
                if (mods == null) {
                    return true; // No mod requirement - unconditional
                }
                for (var element : mods) {
                    if (!loadedMods.test(element.getAsString())) {
                        return false;
                    }
                }
                return true;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read mixin config: " + configName, e);
        }
    }
}
