package com.dhj.actinium.mixins;

import com.dhj.actinium.compat.MixinReEntranceLockFix;
import com.gtnewhorizon.gtnhlib.compat.Mods;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger LOGGER = LogManager.getLogger("Actinium");

    /** Late config whose vanilla target can be pulled in before the config is prepared. */
    private static final String HBM_CONFIG = "mixins.actinium.hbm.json";

    /**
     * Binary name of the vanilla class the hbm config mixes into. Spelled out rather than taken
     * from {@code TileEntityRendererDispatcher.class}: resolving that literal loads the class,
     * destroying the pre-load evidence the diagnostic reports.
     */
    private static final String TILE_ENTITY_RENDERER_DISPATCHER =
        "net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher";

    /**
     * Maps each late/conditional mixin config to the mod ids that gate it.
     * Declared in mixins.actinium.conditions.properties (the mixin loader does
     * not accept custom fields inside the config jsons).
     */
    private static final String CONDITIONS_RESOURCE = "mixins.actinium.conditions.properties";

    private static final Properties CONDITIONAL_CONFIGS = loadConditions();

    @Override
    public List<String> getMixinConfigs() {
        return configsFor(Mods::isModPresent);
    }

    @Override
    public void onMixinConfigQueued(Context context) {
        if (!HBM_CONFIG.equals(context.mixinConfig())) {
            return;
        }
        reportPreloadState();
        preloadTargets(TileEntityRendererDispatcher.class);
    }

    /**
     * Logs whether the hbm config's vanilla target was already loaded when the config was
     * queued, which decides whether {@link #preloadTargets} can still help.
     *
     * <p>The preload only wins while the target is still unknown to the loader; if the tracker
     * already reports it as loaded, the preload is a no-op and the required config will abort
     * the launch. The probe uses the same tracker {@code MixinInfo.readDeclaredTargets} reads,
     * so its answer matches the transformer's decision instead of approximating it.
     */
    private static void reportPreloadState() {
        Boolean alreadyLoaded = MixinReEntranceLockFix.isClassLoaded(TILE_ENTITY_RENDERER_DISPATCHER);
        if (Boolean.TRUE.equals(alreadyLoaded)) {
            LOGGER.warn(
                "[mixin-preload] {} queued, but {} was already loaded: preloading cannot apply it, "
                    + "the config will abort with MixinTargetAlreadyLoadedException",
                HBM_CONFIG,
                TILE_ENTITY_RENDERER_DISPATCHER);
        } else {
            LOGGER.info(
                "[mixin-preload] {} queued, {} alreadyLoaded={}: preloading it so the queued configs apply in one pass",
                HBM_CONFIG,
                TILE_ENTITY_RENDERER_DISPATCHER,
                alreadyLoaded);
        }
    }

    /**
     * Loads a late config's vanilla targets immediately, while no class transform is in
     * progress on this thread and every queued config can still apply to them.
     *
     * <p>Legacy coremod transformers may otherwise pull these classes in re-entrantly (via
     * {@code ClassWriter#getCommonSuperClass} and friends) during another class's mixin
     * application; the re-entrant load then records the target as loaded before the queued
     * config selects, and required configs abort the launch with
     * {@code MixinTargetAlreadyLoadedException}. The class literals must stay inside method
     * bodies: in a static initializer they would load the targets before any config is
     * queued, recreating the very failure this prevents.
     */
    private static void preloadTargets(Class<?>... classes) {
        MixinReEntranceLockFix.clearLeakedLock();
        MixinReEntranceLockFix.clearInvalidVanillaClasses();
        try {
            MixinReEntranceLockFix.preloadClasses(classes);
        } finally {
            MixinReEntranceLockFix.clearLeakedLock();
            MixinReEntranceLockFix.clearInvalidVanillaClasses();
        }
    }

    /**
     * Returns the conditional configs whose gating expression matches the runtime.
     * The value syntax is {@code modA,modB|modC}: comma-separated requirements form an
     * AND group, {@code |} separates alternative groups, and a config loads when any
     * group matches. A requirement is a mod id, or {@code class:<binary name>} to probe
     * for a class instead (for compat layers gated on embedded third-party code rather
     * than a mod container).
     */
    static List<String> configsFor(Predicate<String> loadedMods) {
        return configsFor(loadedMods, MixinLate::classPresent);
    }

    static List<String> configsFor(Predicate<String> loadedMods, Predicate<String> classPresent) {
        List<String> mixins = new ArrayList<>();
        CONDITIONAL_CONFIGS.forEach((config, modList) -> {
            for (String alternative : ((String) modList).split("\\|")) {
                boolean allPresent = true;
                for (String requirement : alternative.split(",")) {
                    if (!requirementMet(requirement.trim(), loadedMods, classPresent)) {
                        allPresent = false;
                        break;
                    }
                }
                if (allPresent) {
                    mixins.add((String) config);
                    break;
                }
            }
        });
        return mixins;
    }

    private static final String CLASS_PREFIX = "class:";

    private static boolean requirementMet(String requirement, Predicate<String> loadedMods, Predicate<String> classPresent) {
        if (requirement.startsWith(CLASS_PREFIX)) {
            return classPresent.test(requirement.substring(CLASS_PREFIX.length()));
        }
        return loadedMods.test(requirement);
    }

    /** Resource-probes the class without initializing it. */
    private static boolean classPresent(String className) {
        String resource = className.replace('.', '/') + ".class";
        return MixinLate.class.getClassLoader().getResource(resource) != null;
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
