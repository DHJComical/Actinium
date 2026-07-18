package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.FlagHook;
import net.caffeinemc.mods.sodium.client.config.ConfigApplyException;
import net.caffeinemc.mods.sodium.client.config.search.BigramSearchIndex;
import net.caffeinemc.mods.sodium.client.config.search.SearchQuerySession;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Frozen configuration model and coordinator for pending, apply, undo, reset, and discard transactions.
 */
public final class Config implements ConfigState {
    private static final Logger LOGGER = LogManager.getLogger("SodiumConfig");
    private final List<ModOptions> modOptions;
    private final Map<ResourceLocation, Option> options = new LinkedHashMap<>();
    private final Map<ResourceLocation, List<FlagHook>> flagHooks = new LinkedHashMap<>();
    private final BigramSearchIndex searchIndex;

    public Config(List<ModOptions> modOptions, Supplier<?> languageProvider) {
        List<ModOptions> resolvedModels = resolveModels(modOptions);
        validateResolvedModels(resolvedModels);
        this.modOptions = List.copyOf(resolvedModels);
        this.searchIndex = new BigramSearchIndex(languageProvider, this::registerSearchSources);
        for (ModOptions owner : this.modOptions) {
            for (Page page : owner.pages()) {
                for (OptionGroup group : page.groups()) {
                    for (Option option : group.options()) {
                        this.options.put(option.getId(), option);
                        option.attach(this);
                    }
                }
            }
            for (FlagHook hook : owner.flagHooks()) {
                for (ResourceLocation trigger : hook.getTriggers()) {
                    this.flagHooks.computeIfAbsent(trigger, ignored -> new ArrayList<>()).add(hook);
                }
            }
        }
        for (Option option : this.options.values()) {
            if (option instanceof StatefulOption<?> statefulOption) {
                statefulOption.loadInitialValue();
            }
        }
    }

    /**
     * Verifies unique option IDs, complete dependencies, and an acyclic dependency graph without touching bindings.
     */
    public static void validateModels(Collection<ModOptions> models) {
        validateResolvedModels(resolveModels(models));
    }

    private static void validateResolvedModels(Collection<ModOptions> models) {
        Map<ResourceLocation, Option> options = collectOptions(models);
        for (Option option : options.values()) {
            for (ResourceLocation dependency : option.getDependencies()) {
                if (isMetaDependency(dependency)) {
                    continue;
                }
                if (!options.containsKey(dependency)) {
                    throw new IllegalArgumentException("Option '" + option.getId()
                            + "' declares missing dependency '" + dependency + "'");
                }
            }
        }
        Map<ResourceLocation, VisitState> states = new LinkedHashMap<>();
        for (ResourceLocation id : options.keySet()) {
            visitDependency(id, options, states, new ArrayList<>());
        }
    }

    private static List<ModOptions> resolveModels(Collection<ModOptions> models) {
        Map<ResourceLocation, Option> locationOptions = collectOptions(models);
        Map<ResourceLocation, OptionOverride> overrides = new LinkedHashMap<>();
        Map<ResourceLocation, OptionOverlay> overlays = new LinkedHashMap<>();
        for (ModOptions owner : models) {
            for (OptionOverride override : owner.overrides()) {
                if (override.target().getNamespace().equals(owner.configId())) {
                    throw new IllegalArgumentException("Config '" + owner.configId()
                            + "' cannot replace its own option '" + override.target() + "'");
                }
                OptionOverride previous = overrides.putIfAbsent(override.target(), override);
                if (previous != null) {
                    throw new IllegalArgumentException("Multiple replacements target option '" + override.target()
                            + "': " + previous.source() + " and " + override.source());
                }
            }
            for (OptionOverlay overlay : owner.overlays()) {
                if (overlay.target().getNamespace().equals(owner.configId())) {
                    throw new IllegalArgumentException("Config '" + owner.configId()
                            + "' cannot overlay its own option '" + overlay.target() + "'");
                }
                OptionOverlay previous = overlays.putIfAbsent(overlay.target(), overlay);
                if (previous != null) {
                    throw new IllegalArgumentException("Multiple overlays target option '" + overlay.target()
                            + "': " + previous.source() + " and " + overlay.source());
                }
            }
        }
        for (OptionOverride override : overrides.values()) {
            if (!locationOptions.containsKey(override.target())) {
                throw new IllegalArgumentException("Replacement from '" + override.source()
                        + "' targets missing option '" + override.target() + "'");
            }
            locationOptions.put(override.target(), override.change());
        }
        for (OptionOverlay overlay : overlays.values()) {
            Map.Entry<ResourceLocation, Option> targetEntry = locationOptions.entrySet().stream()
                    .filter(entry -> entry.getValue().getId().equals(overlay.target()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Overlay from '" + overlay.source()
                            + "' targets missing option '" + overlay.target() + "'"));
            targetEntry.setValue(overlay.change().apply(targetEntry.getValue()));
        }
        Set<ResourceLocation> resolvedIds = new LinkedHashSet<>();
        for (Option option : locationOptions.values()) {
            if (!resolvedIds.add(option.getId())) {
                throw new IllegalArgumentException("Option change produced duplicate option ID: " + option.getId());
            }
        }
        List<ModOptions> resolvedModels = new ArrayList<>();
        for (ModOptions owner : models) {
            List<Page> pages = new ArrayList<>();
            for (Page page : owner.pages()) {
                if (page instanceof OptionPage optionPage) {
                    List<OptionGroup> groups = new ArrayList<>();
                    for (OptionGroup group : optionPage.groups()) {
                        List<Option> options = group.options().stream()
                                .map(option -> locationOptions.get(option.getId()))
                                .toList();
                        groups.add(new OptionGroup(group.name(), options));
                    }
                    pages.add(new OptionPage(optionPage.name(), groups));
                } else {
                    pages.add(page);
                }
            }
            resolvedModels.add(new ModOptions(owner.configId(), owner.name(), owner.version(), owner.theme(),
                    owner.icon(), owner.iconMonochrome(), pages, owner.overrides(), owner.overlays(),
                    owner.flagHooks()));
        }
        return resolvedModels;
    }

    private static Map<ResourceLocation, Option> collectOptions(Collection<ModOptions> models) {
        Map<ResourceLocation, Option> options = new LinkedHashMap<>();
        for (ModOptions owner : models) {
            for (Page page : owner.pages()) {
                for (OptionGroup group : page.groups()) {
                    for (Option option : group.options()) {
                        Option previous = options.putIfAbsent(option.getId(), option);
                        if (previous != null) {
                            throw new IllegalArgumentException("Duplicate option ID: " + option.getId());
                        }
                    }
                }
            }
        }
        return options;
    }

    private enum VisitState {
        VISITING,
        VISITED
    }

    private static void visitDependency(ResourceLocation id, Map<ResourceLocation, Option> options,
                                        Map<ResourceLocation, VisitState> states, List<ResourceLocation> path) {
        VisitState state = states.get(id);
        if (state == VisitState.VISITED) {
            return;
        }
        if (state == VisitState.VISITING) {
            path.add(id);
            throw new IllegalArgumentException("Cyclic option dependency: " + path);
        }
        states.put(id, VisitState.VISITING);
        path.add(id);
        for (ResourceLocation dependency : options.get(id).getDependencies()) {
            if (!isMetaDependency(dependency)) {
                visitDependency(dependency, options, states, new ArrayList<>(path));
            }
        }
        states.put(id, VisitState.VISITED);
    }

    private static boolean isMetaDependency(ResourceLocation id) {
        return id.equals(ConfigState.UPDATE_ON_REBUILD) || id.equals(ConfigState.UPDATE_ON_APPLY);
    }

    /** Returns immutable owner models in explicit registration order. */
    public List<ModOptions> getModOptions() {
        return this.modOptions;
    }

    /** Returns immutable stable option IDs in page traversal order. */
    public Set<ResourceLocation> optionIds() {
        return Collections.unmodifiableSet(this.options.keySet());
    }

    /** Returns an option and verifies its expected model type. */
    public <O extends Option> O getOption(ResourceLocation id, Class<O> optionClass) {
        Option option = this.requireOption(id);
        if (!optionClass.isInstance(option)) {
            throw new IllegalArgumentException("Option '" + id + "' is not a " + optionClass.getSimpleName());
        }
        return optionClass.cast(option);
    }

    /** Returns whether any option owns pending state different from its applied baseline. */
    public boolean hasPendingChanges() {
        return this.options.values().stream().anyMatch(Option::hasChanged);
    }

    /**
     * Validates every changed option before writing any binding, then persists storages and executes hooks.
     */
    public ApplyResult applyChanges() {
        Map<StatefulOption<?>, Object> validatedValues = new LinkedHashMap<>();
        for (Option option : this.options.values()) {
            if (option instanceof StatefulOption<?> statefulOption) {
                try {
                    if (statefulOption.hasChanged()) {
                        validatedValues.put(statefulOption, statefulOption.validateForApply());
                    }
                } catch (RuntimeException exception) {
                    LOGGER.error("Pending value validation failed for option '{}'", statefulOption.getId(), exception);
                    throw new ConfigApplyException(statefulOption.getId(), exception);
                }
            }
        }
        List<OptionSnapshot> snapshots = new ArrayList<>(validatedValues.size());
        for (StatefulOption<?> option : validatedValues.keySet()) {
            try {
                snapshots.add(new OptionSnapshot(option, option.captureBindingValue(), option.capturePendingValue()));
            } catch (RuntimeException exception) {
                LOGGER.error("Failed to snapshot binding for option '{}'", option.getId(), exception);
                throw new ConfigApplyException("binding-snapshot", option.getId(), exception);
            }
        }
        Set<StorageEventHandler> storages = new LinkedHashSet<>();
        Set<ResourceLocation> flags = new LinkedHashSet<>();
        Set<ResourceLocation> changedOptions = new LinkedHashSet<>();
        for (StatefulOption<?> option : validatedValues.keySet()) {
            storages.add(option.getStorage());
            flags.addAll(option.getFlags());
            changedOptions.add(option.getId());
        }
        List<OptionSnapshot> attemptedSaves = new ArrayList<>();
        String phase = "binding-save";
        ResourceLocation activeOption = null;
        try {
            for (OptionSnapshot snapshot : snapshots) {
                activeOption = snapshot.option().getId();
                attemptedSaves.add(snapshot);
                snapshot.option().saveValidatedValue(validatedValues.get(snapshot.option()));
            }
            phase = "storage";
            activeOption = null;
            for (StorageEventHandler storage : storages) {
                storage.afterSave();
            }
            phase = "apply-hook";
            for (StatefulOption<?> option : validatedValues.keySet()) {
                activeOption = option.getId();
                option.runApplyHook();
            }
            phase = "flag-hook";
            activeOption = null;
            this.executeFlagHooks(flags);
            validatedValues.keySet().forEach(StatefulOption::commitAppliedValue);
            return new ApplyResult(changedOptions, flags);
        } catch (RuntimeException exception) {
            LOGGER.error("Config apply failed during {}{}", phase,
                    activeOption == null ? "" : " for option '" + activeOption + "'", exception);
            ConfigApplyException applyException = new ConfigApplyException(phase, activeOption, exception);
            this.rollback(attemptedSaves, applyException);
            throw applyException;
        }
    }

    private void rollback(List<OptionSnapshot> attemptedSaves, ConfigApplyException applyException) {
        Set<StorageEventHandler> rollbackStorages = new LinkedHashSet<>();
        for (int index = attemptedSaves.size() - 1; index >= 0; index--) {
            OptionSnapshot snapshot = attemptedSaves.get(index);
            snapshot.option().restorePendingValue(snapshot.pendingValue());
            rollbackStorages.add(snapshot.option().getStorage());
            try {
                snapshot.option().restoreBindingValue(snapshot.bindingValue());
            } catch (RuntimeException rollbackFailure) {
                LOGGER.error("Failed to roll back binding for option '{}'", snapshot.option().getId(), rollbackFailure);
                applyException.addSuppressed(rollbackFailure);
            }
        }
        for (StorageEventHandler storage : rollbackStorages) {
            try {
                storage.afterSave();
            } catch (RuntimeException rollbackFailure) {
                LOGGER.error("Failed to persist rolled-back configuration storage", rollbackFailure);
                applyException.addSuppressed(rollbackFailure);
            }
        }
    }

    private record OptionSnapshot(StatefulOption<?> option, Object bindingValue, Object pendingValue) {
    }

    private void executeFlagHooks(Set<ResourceLocation> flags) {
        Set<FlagHook> triggered = new LinkedHashSet<>();
        for (ResourceLocation flag : flags) {
            triggered.addAll(this.flagHooks.getOrDefault(flag, List.of()));
        }
        Collection<ResourceLocation> immutableFlags = Collections.unmodifiableSet(flags);
        for (FlagHook hook : triggered) {
            hook.accept(immutableFlags, this);
        }
    }

    /** Restores pending state to the current applied baseline. */
    public void undoChanges() {
        this.options.values().forEach(Option::undo);
    }

    /** Sets every pending state to its option-defined default without persistence. */
    public void resetToDefaults() {
        this.options.values().forEach(Option::resetToDefault);
    }

    /** Discards all unapplied edits when the owning screen closes. */
    public void discardChanges() {
        this.undoChanges();
    }

    /** Starts a language-aware search query. */
    public SearchQuerySession startSearchQuery() {
        return this.searchIndex.startQuery();
    }

    private void registerSearchSources() {
        this.modOptions.forEach(options -> options.registerTextSources(this.searchIndex));
    }

    @Override
    public boolean readBooleanOption(ResourceLocation id) {
        return this.getOption(id, BooleanOption.class).getPendingValue();
    }

    /** Reads the committed boolean baseline for UPDATE_ON_APPLY providers. */
    public boolean readAppliedBooleanOption(ResourceLocation id) {
        return this.getOption(id, BooleanOption.class).getAppliedValue();
    }

    @Override
    public int readIntOption(ResourceLocation id) {
        return this.getOption(id, IntegerOption.class).getPendingValue();
    }

    /** Reads the committed integer baseline for UPDATE_ON_APPLY providers. */
    public int readAppliedIntOption(ResourceLocation id) {
        return this.getOption(id, IntegerOption.class).getAppliedValue();
    }

    @Override
    public <E extends Enum<E>> E readEnumOption(ResourceLocation id, Class<E> enumClass) {
        Option option = this.requireOption(id);
        if (!(option instanceof EnumOption<?> enumOption) || enumOption.getEnumClass() != enumClass) {
            throw new IllegalArgumentException("Option '" + id + "' is not enum type " + enumClass.getSimpleName());
        }
        Object value = enumOption.getPendingValue();
        return enumClass.cast(value);
    }

    /** Reads the committed enum baseline for UPDATE_ON_APPLY providers. */
    public <E extends Enum<E>> E readAppliedEnumOption(ResourceLocation id, Class<E> enumClass) {
        Option option = this.requireOption(id);
        if (!(option instanceof EnumOption<?> enumOption) || enumOption.getEnumClass() != enumClass) {
            throw new IllegalArgumentException("Option '" + id + "' is not enum type " + enumClass.getSimpleName());
        }
        return enumClass.cast(enumOption.getAppliedValue());
    }

    private Option requireOption(ResourceLocation id) {
        Option option = this.options.get(id);
        if (option == null) {
            throw new IllegalArgumentException("Unknown option ID: " + id);
        }
        return option;
    }

    /** Immutable summary consumed by GUI-level reload coordination. */
    public record ApplyResult(Set<ResourceLocation> changedOptions, Set<ResourceLocation> flags) {
        public ApplyResult {
            changedOptions = Collections.unmodifiableSet(new LinkedHashSet<>(changedOptions));
            flags = Collections.unmodifiableSet(new LinkedHashSet<>(flags));
        }
    }
}
