package com.dhj.actinium.debug;

import com.dhj.actinium.config.ActiniumRuntimeOptions;
import com.dhj.actinium.loading.ActiniumLateMixinLoader;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.core.CeleritasLoadingPlugin;

import me.decce.gnetum.mixins.LateMixinLoader;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ActiniumDiagnostics {
    private static final Logger LOGGER = LogManager.getLogger("ActiniumDiagnostics");
    private static final Set<String> APPLIED_MIXINS = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Set<String> GIBBED_RENDER_PATHS = Collections.synchronizedSet(new LinkedHashSet<>());

    private ActiniumDiagnostics() {
    }

    public static boolean isEnabled() {
        String override = System.getProperty("actinium.productionDiagnostics");
        if (override != null) {
            return Boolean.parseBoolean(override);
        }

        try {
            return CeleritasVintage.options().debug.enableProductionDiagnostics;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    public static void logConstruction() {
        if (!isEnabled()) {
            return;
        }

        LOGGER.info(
            "construction env={} side=client java={} os={} coremod={} mixinConfigs={}",
            isDeobfuscatedEnvironment() ? "dev" : "production",
            System.getProperty("java.version"),
            System.getProperty("os.name") + " " + System.getProperty("os.version"),
            System.getProperty("fml.coreMods.load", ""),
            describeMixinConfigs()
        );
    }

    public static void logInitialization(String version) {
        if (!isEnabled()) {
            return;
        }

        LOGGER.info("init version={} config={}", version, describeConfig());
    }

    public static void recordMixinApplied(String targetClassName, String mixinClassName) {
        if (!isEnabled() || !isKeyMixin(mixinClassName)) {
            return;
        }

        String entry = mixinClassName + " -> " + targetClassName;
        if (APPLIED_MIXINS.add(entry)) {
            LOGGER.info("mixin-applied {}", entry);
        }
    }

    public static void recordGibbedRenderPath(String path) {
        if (!isEnabled()) {
            return;
        }

        if (GIBBED_RENDER_PATHS.add(path)) {
            LOGGER.info("gibbed-render-path {}", path);
        }
    }

    private static boolean isKeyMixin(String mixinClassName) {
        return mixinClassName.startsWith("com.dhj.actinium.mixin.features.iris.")
            || mixinClassName.startsWith("com.dhj.actinium.mixin.mod.gibbed.")
            || mixinClassName.startsWith("me.decce.gnetum.mixins.")
            || mixinClassName.startsWith("com.dhj.actinium.mixin.core.terrain.")
            || mixinClassName.startsWith("org.taumc.celeritas.mixin.core.");
    }

    private static String describeMixinConfigs() {
        String earlyConfigs = String.join(",", CeleritasLoadingPlugin.getEarlyMixinConfigs());
        String actiniumLateConfigs = String.join(",", new ActiniumLateMixinLoader().getMixinConfigs());
        String gnetumLateConfigs = String.join(",", new LateMixinLoader().getMixinConfigs());

        if (actiniumLateConfigs.isEmpty()) {
            return earlyConfigs + "," + gnetumLateConfigs;
        }

        return earlyConfigs + "," + actiniumLateConfigs + "," + gnetumLateConfigs;
    }

    private static String describeConfig() {
        try {
            var options = CeleritasVintage.options();
            return "advanced{multiDraw=" + options.advanced.multiDrawMode
                + ",streaming=" + options.advanced.streamingUploadStrategy
                + ",directMemory=" + ActiniumRuntimeOptions.allowDirectMemoryAccess()
                + ",modelRendererBatching=" + ActiniumRuntimeOptions.useModelRendererBatching()
                + ",modelRendererDisplayLists=" + ActiniumRuntimeOptions.useModelRendererDisplayLists()
                + ",fastLitItemRendering=" + ActiniumRuntimeOptions.useFastLitItemRendering()
                + ",fastLitItemDisplayLists=" + ActiniumRuntimeOptions.useFastLitItemDisplayLists()
                + "} debug{gl=" + options.debug.enableActiniumGlDebug
                + ",perf=" + options.debug.enableActiniumPerfDebug
                + ",gpuPerf=" + options.debug.enableActiniumGpuPerfDebug
                + ",preGlError=" + options.debug.enableFrameGlErrorCheck
                + ",postGlError=" + options.debug.enablePostRenderGlErrorCheck
                + ",diagnostics=" + options.debug.enableProductionDiagnostics
                + ",redirectorDebug=" + options.debug.enableRedirectorDebug
                + ",redirectorLogSpam=" + options.debug.enableRedirectorLogSpam
                + ",redirectorClassDump=" + options.debug.enableRedirectorClassDump
                + "}";
        } catch (RuntimeException e) {
            return "unavailable:" + e.getClass().getSimpleName();
        }
    }

    private static boolean isDeobfuscatedEnvironment() {
        Object value = Launch.blackboard.get("fml.deobfuscatedEnvironment");
        return value instanceof Boolean && (Boolean) value;
    }
}
