package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.api.options.structure.OptionFlag;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;

import java.util.Set;
import java.util.stream.Collectors;

final class CurrentStorageMapper {
    private CurrentStorageMapper() {
    }

    static StorageModel<?> describe(OptionStorage<?> source) {
        return describeCaptured(source);
    }

    private static <T> StorageModel<T> describeCaptured(OptionStorage<T> source) {
        return new StorageModel<>() {
            @Override
            public T getData() {
                return source.getData();
            }

            @Override
            public void save() {
                source.save();
            }

            @Override
            public void save(Set<String> flags) {
                source.save(toFlags(flags));
            }
        };
    }

    static <T> OptionStorage<T> create(StorageModel<T> source) {
        return new OptionStorage<>() {
            @Override
            public T getData() {
                return source.getData();
            }

            @Override
            public void save() {
                source.save();
            }

            @Override
            public void save(Set<OptionFlag> flags) {
                source.save(toNames(flags));
            }
        };
    }

    static Set<String> toNames(Set<OptionFlag> flags) {
        return flags.stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }

    static OptionFlag[] toArray(Set<String> flags) {
        return flags.stream().map(OptionFlag::valueOf).toArray(OptionFlag[]::new);
    }

    static Set<OptionFlag> toFlags(Set<String> flags) {
        return flags.stream().map(OptionFlag::valueOf).collect(Collectors.toUnmodifiableSet());
    }
}
