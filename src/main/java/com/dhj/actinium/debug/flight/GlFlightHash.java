package com.dhj.actinium.debug.flight;

import java.util.Objects;

/**
 * Produces stable allocation-free identifiers for runtime labels that cannot be stored as text.
 */
public final class GlFlightHash {
    private static final long FNV_OFFSET_BASIS = 0xCBF29CE484222325L;
    private static final long FNV_PRIME = 0x100000001B3L;

    private GlFlightHash() {
    }

    /**
     * Hashes UTF-16 code units with FNV-1a so values remain stable across processes and JVM versions.
     *
     * @param value label to hash
     * @return stable 64-bit hash
     */
    public static long stableHash(CharSequence value) {
        Objects.requireNonNull(value, "value");
        long hash = FNV_OFFSET_BASIS;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            hash ^= character & 0xFFL;
            hash *= FNV_PRIME;
            hash ^= character >>> 8;
            hash *= FNV_PRIME;
        }
        return hash;
    }

    /**
     * Uses zero as the stable sentinel for an absent dimension key.
     *
     * @param value nullable label
     * @return zero for null, otherwise {@link #stableHash(CharSequence)}
     */
    public static long stableNullableHash(CharSequence value) {
        return value == null ? 0L : stableHash(value);
    }
}
