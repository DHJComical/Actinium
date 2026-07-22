package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.Control;
import org.taumc.celeritas.api.options.structure.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Maps the legacy Celeritas option model to and from neutral bridge models.
 */
final class LegacyOptionMapper {
    private LegacyOptionMapper() {
    }

    static <T> IdentifierModel<T> describeIdentifier(OptionIdentifier<T> id) {
        return new IdentifierModel<>(id.getModId(), id.getPath(), id.getType());
    }

    static <T> OptionIdentifier<T> createIdentifier(IdentifierModel<T> id) {
        return OptionIdentifier.create(id.namespace(), id.path(), id.type());
    }

    static OptionModel<?> describeOption(Option<?> source) {
        return describeCaptured(source);
    }

    private static <T> OptionModel<T> describeCaptured(Option<T> source) {
        IdentifierModel<T> id = describeIdentifier(source.getId());
        ControlModel<T> control = LegacyControlMapper.describe(source.getControl());
        StorageModel<?> storage = LegacyStorageMapper.describe(source.getStorage());
        Set<String> flags = LegacyStorageMapper.toNames(source.getFlags());
        return new OptionModel<>() {
            @Override
            public IdentifierModel<T> id() {
                return id;
            }

            @Override
            public TextComponent name() {
                return source.getName();
            }

            @Override
            public TextComponent tooltip() {
                return source.getTooltip();
            }

            @Override
            public String impactName() {
                return source.getImpact() == null ? null : source.getImpact().name();
            }

            @Override
            public ControlModel<T> control() {
                return control;
            }

            @Override
            public StorageModel<?> storage() {
                return storage;
            }

            @Override
            public T getValue() {
                return source.getValue();
            }

            @Override
            public void setValue(T value) {
                source.setValue(value);
            }

            @Override
            public void reset() {
                source.reset();
            }

            @Override
            public boolean isAvailable() {
                return source.isAvailable();
            }

            @Override
            public boolean hasChanged() {
                return source.hasChanged();
            }

            @Override
            public void applyChanges() {
                source.applyChanges();
            }

            @Override
            public Set<String> flagNames() {
                return flags;
            }
        };
    }

    static Option<?> createOptionView(OptionModel<?> source) {
        return createViewCaptured(source);
    }

    private static <T> Option<T> createViewCaptured(OptionModel<T> source) {
        return createViewWithStorage(source, source.storage());
    }

    private static <S, T> Option<T> createViewWithStorage(OptionModel<T> source, StorageModel<S> storageModel) {
        OptionStorage<S> storage = LegacyStorageMapper.create(storageModel);
        return new Option<>() {
            @Override
            public OptionIdentifier<T> getId() {
                return createIdentifier(source.id());
            }

            @Override
            public TextComponent getName() {
                return source.name();
            }

            @Override
            public TextComponent getTooltip() {
                return source.tooltip();
            }

            @Override
            public OptionImpact getImpact() {
                return source.impactName() == null ? null : OptionImpact.valueOf(source.impactName());
            }

            @Override
            public Control<T> getControl() {
                return LegacyControlMapper.create(this, source.control());
            }

            @Override
            public T getValue() {
                return source.getValue();
            }

            @Override
            public void setValue(T value) {
                source.setValue(value);
            }

            @Override
            public void reset() {
                source.reset();
            }

            @Override
            public OptionStorage<?> getStorage() {
                return storage;
            }

            @Override
            public boolean isAvailable() {
                return source.isAvailable();
            }

            @Override
            public boolean hasChanged() {
                return source.hasChanged();
            }

            @Override
            public void applyChanges() {
                source.applyChanges();
            }

            @Override
            public Collection<OptionFlag> getFlags() {
                return LegacyStorageMapper.toFlags(source.flagNames());
            }
        };
    }

    static OptionGroupModel describeGroup(OptionGroup source) {
        List<OptionModel<?>> options = source.getOptions().stream()
                .<OptionModel<?>>map(LegacyOptionMapper::describeOption)
                .toList();
        return new OptionGroupModel(describeIdentifier(source.getId()), options);
    }

    static OptionGroup createGroupView(OptionGroupModel source) {
        List<Option<?>> options = source.options().stream()
                .<Option<?>>map(LegacyOptionMapper::createOptionView)
                .toList();
        return new LegacyOptionGroupView(createIdentifier(source.id()), options);
    }

    static OptionPageModel describePage(OptionPage source) {
        List<OptionGroupModel> groups = source.getGroups().stream()
                .map(LegacyOptionMapper::describeGroup)
                .toList();
        return new OptionPageModel(describeIdentifier(source.getId()), source.getName(), groups);
    }

    static OptionPage createPageView(OptionPageModel source) {
        List<OptionGroup> groups = source.groups().stream()
                .map(LegacyOptionMapper::createGroupView)
                .toList();
        return new LegacyOptionPageView(createIdentifier(source.id()), source.name(), groups);
    }
}
