package com.dhj.actinium.mixins;

import com.dhj.actinium.compat.MixinReEntranceLockFix;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.Context;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class MixinLate implements ILateMixinLoader {

    /**
     * Maps each late/conditional mixin config to the mod ids that gate it.
     * Declared in mixins.actinium.conditions.properties (the mixin loader does
     * not accept custom fields inside the config jsons).
     */
    private static final String CONDITIONS_RESOURCE = "mixins.actinium.conditions.properties";

    private static final Properties CONDITIONAL_CONFIGS = loadConditions();

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
        CONDITIONAL_CONFIGS.forEach((config, modList) -> {
            boolean allLoaded = true;
            for (String modId : ((String) modList).split(",")) {
                if (!loadedMods.test(modId.trim())) {
                    allLoaded = false;
                    break;
                }
            }
            if (allLoaded) {
                mixins.add((String) config);
            }
        });
        return mixins;
    }

    private static Properties loadConditions() {
        final Properties properties = new Properties();
        try (InputStream stream = MixinLate.class.getClassLoader().getResourceAsStream(CONDITIONS_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing mixin condition declarations: " + CONDITIONS_RESOURCE);
            }
            properties.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read mixin condition declarations: " + CONDITIONS_RESOURCE, e);
        }
        return properties;
    }
}
