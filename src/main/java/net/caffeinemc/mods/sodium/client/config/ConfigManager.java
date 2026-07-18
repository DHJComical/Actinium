package net.caffeinemc.mods.sodium.client.config;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.client.config.builder.ConfigBuilderImpl;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Explicit, single-use configuration registry replacing upstream classpath and reflection discovery.
 */
public final class ConfigManager {
    private static final Logger LOGGER = LogManager.getLogger("SodiumConfig");
    private final Function<String, ModMetadata> metadataProvider;
    private final Supplier<?> languageProvider;
    private final List<ConfigUser> users = new ArrayList<>();
    private final List<RegistrationFailure> failures = new ArrayList<>();
    private boolean frozen;
    private Config config;

    public ConfigManager(Function<String, ModMetadata> metadataProvider, Supplier<?> languageProvider) {
        this.metadataProvider = metadataProvider;
        this.languageProvider = languageProvider;
    }

    /** Registers an already constructed entry point under an auditable source ID. */
    public void registerConfigEntryPoint(String source, ConfigEntryPoint entryPoint) {
        this.registerConfigEntryPoint(source, entryPoint, false);
    }

    /** Registers a required built-in entry point whose failure must abort configuration initialization. */
    public void registerCoreConfigEntryPoint(String source, ConfigEntryPoint entryPoint) {
        this.registerConfigEntryPoint(source, entryPoint, true);
    }

    private void registerConfigEntryPoint(String source, ConfigEntryPoint entryPoint, boolean core) {
        if (this.frozen) {
            throw new IllegalStateException("Config registry is already frozen; late source: " + source);
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Config entry point source must not be blank");
        }
        if (entryPoint == null) {
            throw new IllegalArgumentException("Config entry point must not be null for source '" + source + "'");
        }
        this.users.add(new ConfigUser(source, entryPoint, core));
    }

    /**
     * Builds contributions in registration order, isolates a failing source, and permanently freezes the registry.
     */
    public Config freeze() {
        if (this.frozen) {
            return this.config;
        }
        List<ModOptions> accepted = new ArrayList<>();
        Set<String> configIds = new LinkedHashSet<>();
        for (ConfigUser user : this.users) {
            ConfigBuilderImpl builder = new ConfigBuilderImpl(this.metadataProvider, user.source());
            try {
                user.entryPoint().registerConfigEarly(builder);
                user.entryPoint().registerConfigLate(builder);
                List<ModOptions> contribution = builder.build();
                for (ModOptions options : contribution) {
                    if (configIds.contains(options.configId())) {
                        throw new IllegalArgumentException("Duplicate config ID '" + options.configId()
                                + "' registered by source '" + user.source() + "'");
                    }
                }
                List<ModOptions> candidate = new ArrayList<>(accepted);
                candidate.addAll(contribution);
                Config.validateModels(candidate);
                accepted.addAll(contribution);
                contribution.forEach(options -> configIds.add(options.configId()));
            } catch (RuntimeException exception) {
                if (user.core()) {
                    LOGGER.error("Core config entry point '{}' failed", user.source(), exception);
                    throw exception;
                }
                LOGGER.error("Config entry point '{}' failed and was isolated", user.source(), exception);
                this.failures.add(new RegistrationFailure(user.source(), exception));
            }
        }
        this.config = new Config(accepted, this.languageProvider);
        this.frozen = true;
        return this.config;
    }

    /** Returns immutable registration failures after freeze. */
    public List<RegistrationFailure> failures() {
        return List.copyOf(this.failures);
    }

    /** Metadata required by the upstream-style convenience builder. */
    public record ModMetadata(String modName, String modVersion) {
    }

    /** Source-scoped registration failure retained for diagnostics. */
    public record RegistrationFailure(String source, RuntimeException cause) {
    }

    private record ConfigUser(String source, ConfigEntryPoint entryPoint, boolean core) {
    }
}
