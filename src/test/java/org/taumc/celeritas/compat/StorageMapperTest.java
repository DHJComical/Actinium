package org.taumc.celeritas.compat;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StorageMapperTest {
    @Test
    void forwardsLegacyFlagsToCurrentStorage() {
        RecordingStorage storage = new RecordingStorage();
        var current = CurrentStorageMapper.create(storage);
        var legacy = LegacyStorageMapper.create(CurrentStorageMapper.describe(current));

        legacy.save(LegacyStorageMapper.toFlags(Set.of("REQUIRES_RENDERER_RELOAD")));

        assertEquals(Set.of("REQUIRES_RENDERER_RELOAD"), storage.flags);
        assertSame(storage.data, legacy.getData());
    }

    @Test
    void forwardsCurrentFlagsToLegacyStorage() {
        RecordingStorage storage = new RecordingStorage();
        var legacy = LegacyStorageMapper.create(storage);
        var current = CurrentStorageMapper.create(LegacyStorageMapper.describe(legacy));

        current.save(CurrentStorageMapper.toFlags(Set.of("REQUIRES_ASSET_RELOAD")));

        assertEquals(Set.of("REQUIRES_ASSET_RELOAD"), storage.flags);
        assertSame(storage.data, current.getData());

        var legacyOption = LegacyOptionFixtures.option("storage_identity");
        var currentOption = CurrentOptionMapper.createOption(LegacyOptionMapper.describeOption(legacyOption));
        assertSame(legacyOption.getStorage().getData(), currentOption.getStorage().getData());
    }

    @Test
    void forwardsUnflaggedSavesInBothDirections() {
        RecordingStorage storage = new RecordingStorage();
        LegacyStorageMapper.create(CurrentStorageMapper.describe(CurrentStorageMapper.create(storage))).save();
        CurrentStorageMapper.create(LegacyStorageMapper.describe(LegacyStorageMapper.create(storage))).save();
        assertEquals(2, storage.unflaggedSaves);
    }

    private static final class RecordingStorage implements StorageModel<Object> {
        private final Object data = new Object();
        private Set<String> flags = Set.of();
        private int unflaggedSaves;

        @Override
        public Object getData() {
            return data;
        }

        @Override
        public void save() {
            unflaggedSaves++;
        }

        @Override
        public void save(Set<String> flags) {
            this.flags = Set.copyOf(flags);
        }
    }
}
