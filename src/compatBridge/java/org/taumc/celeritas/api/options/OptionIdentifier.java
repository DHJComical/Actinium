package org.taumc.celeritas.api.options;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Stable, interned identifier used by the legacy Celeritas option API.
 */
public final class OptionIdentifier<T> {
    private final String modId;
    private final String path;
    private final Class<T> type;
    private static final ObjectOpenHashSet<OptionIdentifier<?>> IDENTIFIERS = new ObjectOpenHashSet<>();
    public static final OptionIdentifier<Void> EMPTY = create("", "", Void.class);

    private OptionIdentifier(String modId, String path, Class<T> type) {
        this.modId = modId;
        this.path = path;
        this.type = type;
    }

    public String getModId() {
        return modId;
    }

    public String getPath() {
        return path;
    }

    public Class<T> getType() {
        return type;
    }

    public static OptionIdentifier<Void> create(String modId, String path) {
        return create(modId, path, void.class);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> OptionIdentifier<T> create(String modId, String path, Class<T> type) {
        if (modId.equals("embeddium")) {
            modId = "celeritas";
        }
        OptionIdentifier<T> candidate = new OptionIdentifier<>(modId, path, type);
        OptionIdentifier<T> identifier = (OptionIdentifier<T>) IDENTIFIERS.addOrGet(candidate);
        if (identifier != null && identifier.type != candidate.type) {
            throw new IllegalArgumentException(String.format(
                    "OptionIdentifier '%s' created with differing class type %s from existing instance %s",
                    candidate, candidate.type, identifier.type));
        }
        return identifier;
    }

    public static boolean isPresent(@Nullable OptionIdentifier<?> id) {
        return id != null && id != EMPTY;
    }

    public boolean matches(OptionIdentifier<?> other) {
        return this == other;
    }

    @SuppressWarnings("unchecked")
    public <U> OptionIdentifier<U> cast() {
        return (OptionIdentifier<U>) this;
    }

    @Override
    public String toString() {
        return modId + ":" + path;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        OptionIdentifier<?> that = (OptionIdentifier<?>) other;
        return Objects.equals(modId, that.modId) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId, path);
    }
}
