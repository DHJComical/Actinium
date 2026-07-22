package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.structure.*;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Maps the current Embeddium option model to and from neutral bridge models.
 */
final class CurrentOptionMapper {
    private CurrentOptionMapper() {
    }

    static <T> IdentifierModel<T> describeIdentifier(OptionIdentifier<T> id) {
        if (id.getModId().equals("actinium") && id.getPath().equals("fullscreen_resolution")) {
            return new IdentifierModel<>("minecraft", "fullscreen_resolution", id.getType());
        }
        return new IdentifierModel<>(id.getModId(), id.getPath(), id.getType());
    }

    static <T> OptionIdentifier<T> createIdentifier(IdentifierModel<T> id) {
        if (id.namespace().equals("minecraft") && id.path().equals("fullscreen_resolution")) {
            return OptionIdentifier.create("actinium", "fullscreen_resolution", id.type());
        }
        return OptionIdentifier.create(id.namespace(), id.path(), id.type());
    }

    static OptionModel<?> describeOption(Option<?> source) {
        return describeCaptured(source);
    }

    private static <T> OptionModel<T> describeCaptured(Option<T> source) {
        IdentifierModel<T> id = describeIdentifier(source.getId());
        ControlModel<T> control = CurrentControlMapper.describe(source.getControl());
        StorageModel<?> storage = CurrentStorageMapper.describe(source.getStorage());
        Set<String> flags = CurrentStorageMapper.toNames(Set.copyOf(source.getFlags()));
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

    static Option<?> createOption(OptionModel<?> source) {
        return createCaptured(source);
    }

    private static <T> Option<T> createCaptured(OptionModel<T> source) {
        return createWithStorage(source, source.storage());
    }

    private static <S, T> Option<T> createWithStorage(OptionModel<T> source, StorageModel<S> storageModel) {
        var storage = CurrentStorageMapper.create(storageModel);
        OptionImpl.Builder<S, T> builder = OptionImpl.createBuilder(source.id().type(), storage)
                .setId(createIdentifier(source.id()))
                .setName(source.name())
                .setTooltip(source.tooltip())
                .setBinding((data, value) -> {
                    source.setValue(value);
                    source.applyChanges();
                }, data -> source.getValue())
                .setEnabledPredicate(source::isAvailable)
                .setFlags(CurrentStorageMapper.toArray(source.flagNames()));
        if (source.impactName() != null) {
            builder.setImpact(OptionImpact.valueOf(source.impactName()));
        }
        builder.setControl(option -> CurrentControlMapper.create(option, source.control()));
        return builder.build();
    }

    static OptionGroupModel describeGroup(OptionGroup source) {
        List<OptionModel<?>> options = source.getOptions().stream()
                .<OptionModel<?>>map(CurrentOptionMapper::describeOption)
                .toList();
        return new OptionGroupModel(describeIdentifier(source.getId()), options);
    }

    static OptionGroup createGroup(OptionGroupModel source) {
        return BridgeDispatchGuard.INSTANCE.suppress(() -> {
            OptionGroup.Builder builder = OptionGroup.createBuilder().setId(createIdentifier(source.id()));
            for (OptionModel<?> option : source.options()) {
                builder.add(createOption(option));
            }
            return builder.build();
        });
    }

    static OptionPageModel describePage(OptionPage source) {
        List<OptionGroupModel> groups = source.getGroups().stream()
                .map(CurrentOptionMapper::describeGroup)
                .toList();
        return new OptionPageModel(describeIdentifier(source.getId()), source.getName(), groups);
    }

    static OptionPage createPage(OptionPageModel source) {
        return BridgeDispatchGuard.INSTANCE.suppress(() -> {
            List<OptionGroup> groups = new ArrayList<>();
            for (OptionGroupModel group : source.groups()) {
                groups.add(createGroup(group));
            }
            return new OptionPage(createIdentifier(source.id()), source.name(), groups);
        });
    }
}
