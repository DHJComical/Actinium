package com.dhj.actinium.compat.sodium;

import com.dhj.actinium.runtime.ActiniumRuntime;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.irisshaders.iris.compat.sodium.IrisConfigEntryPoint;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Owns the shared explicit Config registry used by the Actinium settings screen and later Iris registration.
 */
public final class ActiniumConfigBootstrap {
    private static final Logger LOGGER = LogManager.getLogger("Actinium-Config");
    private static final class SharedHolder {
        private static final ActiniumConfigBootstrap INSTANCE = ActiniumConfigBootstrap.builder().build();
    }

    private final ConfigManager configManager;
    private Config config;

    private ActiniumConfigBootstrap(Builder builder) {
        this.configManager = new ConfigManager(builder.metadataProvider, builder.languageProvider);
        List<OptionPage> builtInPages = ActiniumConfigEntryPoint.createLegacyPages();
        this.configManager.registerCoreConfigEntryPoint(ActiniumRuntime.MODID,
                new ActiniumConfigEntryPoint(builtInPages, new ActiniumFlagHook(builder.applyActions)));
        if (builder.bridgeLegacyExtensions) {
            Map<String, List<OptionPage>> extensions = OptionGUIConstructionBridge.collectExtensions(builtInPages);
            extensions.forEach((namespace, pages) -> this.configManager.registerConfigEntryPoint(
                    "legacy-option-gui:" + namespace,
                    new LegacyExtensionEntryPoint(namespace, pages)));
        }
        registerIrisIfAvailable();
    }

    /** Registers Iris before the shared Config is frozen, isolating optional integration failures. */
    private void registerIrisIfAvailable() {
        try {
            // Register the owner even when shader rendering is disabled so the page remains discoverable.
            // IrisConfigEntryPoint gates controls and rejects opening shader packs while disabled.
            registerConfigEntryPoint("iris", new IrisConfigEntryPoint());
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to register Iris Config entrypoint; continuing without Iris options", exception);
        }
    }

    /** Returns the process-wide bootstrap without freezing it before first Config access. */
    public static ActiniumConfigBootstrap shared() {
        return SharedHolder.INSTANCE;
    }

    /** Creates a configurable bootstrap for integration tests or alternate platform ownership. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Registers an explicit extension, such as Iris, before the shared registry is frozen.
     */
    public synchronized void registerConfigEntryPoint(String source, ConfigEntryPoint entryPoint) {
        if (this.config != null) {
            throw new IllegalStateException("Cannot register Config entry point '" + source
                    + "' after the Actinium Config has been frozen");
        }
        this.configManager.registerConfigEntryPoint(source, entryPoint);
    }

    /** Freezes on first access and returns the same Config instance for every settings screen. */
    public synchronized Config config() {
        if (this.config == null) {
            this.config = this.configManager.freeze();
        }
        return this.config;
    }

    /** Returns isolated third-party failures after the registry has frozen. */
    public List<ConfigManager.RegistrationFailure> registrationFailures() {
        return this.configManager.failures();
    }

    /** Builder keeps bootstrap construction explicit while allowing logic tests to replace runtime actions. */
    public static final class Builder {
        private Function<String, ConfigManager.ModMetadata> metadataProvider = id ->
                new ConfigManager.ModMetadata(id, "unknown");
        private Supplier<?> languageProvider = () -> Minecraft.getMinecraft().getLanguageManager()
                .getCurrentLanguage().getLanguageCode();
        private ActiniumApplyActions applyActions = new ActiniumApplyActionsImpl(Minecraft.getMinecraft());
        private boolean bridgeLegacyExtensions = true;

        private Builder() {
        }

        /** Supplies metadata for convenience entrypoints that do not declare it directly. */
        public Builder setMetadataProvider(Function<String, ConfigManager.ModMetadata> metadataProvider) {
            if (metadataProvider == null) {
                throw new IllegalArgumentException("Metadata provider must not be null");
            }
            this.metadataProvider = metadataProvider;
            return this;
        }

        /** Supplies the language identity used to invalidate the search index. */
        public Builder setLanguageProvider(Supplier<?> languageProvider) {
            if (languageProvider == null) {
                throw new IllegalArgumentException("Language provider must not be null");
            }
            this.languageProvider = languageProvider;
            return this;
        }

        /** Supplies the concrete action boundary for applied option flags. */
        public Builder setApplyActions(ActiniumApplyActions applyActions) {
            if (applyActions == null) {
                throw new IllegalArgumentException("Apply actions must not be null");
            }
            this.applyActions = applyActions;
            return this;
        }

        /** Controls legacy event collection for deterministic integration tests. */
        public Builder bridgeLegacyExtensions(boolean bridgeLegacyExtensions) {
            this.bridgeLegacyExtensions = bridgeLegacyExtensions;
            return this;
        }

        /** Builds an unfrozen registry with the required Actinium core entrypoint already registered. */
        public ActiniumConfigBootstrap build() {
            return new ActiniumConfigBootstrap(this);
        }
    }
}
